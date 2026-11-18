import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit
import WhiteHatClient

private enum PlatformError: Error {
    case invalidConfiguration, invalidIdentityToken
}

struct HostConfiguration {
    let api: String
    let issuer: String
    let clientID: String
    let authorization: String
    let token: String
    let revocation: String
    let redirect: String
    let jwks: URL

    init(bundle: Bundle = .main) throws {
        func setting(_ name: String) throws -> String {
            guard let value = bundle.object(forInfoDictionaryKey: "WhiteHat" + name) as? String,
                  !value.isEmpty else { throw PlatformError.invalidConfiguration }
            return value
        }
        func endpoint(_ name: String) throws -> URL {
            guard let url = URL(string: try setting(name)), url.scheme == "https",
                  let host = url.host, !host.isEmpty, !host.hasSuffix(".invalid"),
                  url.user == nil, url.password == nil, url.fragment == nil else {
                throw PlatformError.invalidConfiguration
            }
            return url
        }
        api = try endpoint("API").absoluteString
        issuer = try endpoint("Issuer").absoluteString
        clientID = try setting("ClientID")
        authorization = try endpoint("Authorization").absoluteString
        token = try endpoint("Token").absoluteString
        revocation = try endpoint("Revocation").absoluteString
        let callback = try endpoint("Redirect")
        guard callback.port == nil, callback.query == nil, callback.path == "/oauth/ios" else {
            throw PlatformError.invalidConfiguration
        }
        redirect = callback.absoluteString
        jwks = try endpoint("JWKS")
    }
}

private final class NoRedirect: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}

final class AppleHostPlatform: NSObject, IosHostPlatform, ASWebAuthenticationPresentationContextProviding {
    private let config: HostConfiguration
    private let keychain: [String: Any]
    private let network: URLSession
    private var browser: ASWebAuthenticationSession?
    private var browserAttempt: UUID?
    private var browserTimeout: Task<Void, Never>?
    private var anchor: ASPresentationAnchor?

    init(config: HostConfiguration) {
        self.config = config
        keychain = [kSecClass as String: kSecClassGenericPassword,
                    kSecAttrService as String: Bundle.main.bundleIdentifier ?? "whitehat.ios",
                    kSecAttrAccount as String: config.issuer + "|" + config.clientID]
        let options = URLSessionConfiguration.ephemeral
        options.timeoutIntervalForRequest = 15
        options.timeoutIntervalForResource = 20
        network = URLSession(configuration: options, delegate: NoRedirect(), delegateQueue: nil)
    }

    deinit { network.invalidateAndCancel() }

    func randomUrlToken() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = bytes.withUnsafeMutableBytes { buffer in
            SecRandomCopyBytes(kSecRandomDefault, buffer.count, buffer.baseAddress!)
        }
        return status == errSecSuccess ? Data(bytes).base64URL : ""
    }

    func s256(verifier: String) -> String {
        Data(SHA256.hash(data: Data(verifier.utf8))).base64URL
    }

    func authorize(url: String, redirect: String, reply: IosHostReply) {
        DispatchQueue.main.async {
            guard self.browser == nil, let request = URL(string: url), request.scheme == "https",
                  let callback = URL(string: redirect), callback.absoluteString == self.config.redirect,
                  let host = callback.host,
                  let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                    .filter({ $0.activationState == .foregroundActive }).flatMap({ $0.windows })
                    .first(where: { $0.isKeyWindow }) else {
                reply.complete(value: nil, error: "Could not open the system browser.")
                return
            }
            let attempt = UUID()
            let session = ASWebAuthenticationSession(url: request, callback: .https(host: host, path: callback.path)) {
                [weak self] result, error in
                DispatchQueue.main.async {
                    self?.finish(attempt, reply: reply, value: result?.absoluteString,
                                 error: error == nil ? nil : "Sign-in was cancelled or failed.")
                }
            }
            self.browser = session
            self.browserAttempt = attempt
            self.anchor = window
            session.presentationContextProvider = self
            if session.start() {
                self.browserTimeout = Task { @MainActor [weak self] in
                    do { try await Task.sleep(for: .seconds(120)) } catch { return }
                    self?.finish(attempt, reply: reply, value: nil, error: "Sign-in timed out.")
                }
            } else {
                self.finish(attempt, reply: reply, value: nil, error: "Could not start sign-in.")
            }
        }
    }

    private func finish(_ attempt: UUID, reply: IosHostReply, value: String?, error: String?) {
        guard browserAttempt == attempt else { return }
        browserAttempt = nil
        browserTimeout?.cancel()
        browserTimeout = nil
        let session = browser
        browser = nil
        anchor = nil
        session?.cancel()
        reply.complete(value: value, error: error)
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        anchor ?? ASPresentationAnchor()
    }

    func readSession() -> IosHostResult {
        var query = keychain
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return IosHostResult(value: nil, error: nil) }
        guard status == errSecSuccess, let data = result as? Data,
              let text = String(data: data, encoding: .utf8) else {
            return IosHostResult(value: nil, error: "Could not read the protected session.")
        }
        return IosHostResult(value: text, error: nil)
    }

    func writeSession(value: String?) -> IosHostResult {
        let status: OSStatus
        if let value {
            let attributes: [String: Any] = [
                kSecValueData as String: Data(value.utf8),
                kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            ]
            let updated = SecItemUpdate(keychain as CFDictionary, attributes as CFDictionary)
            status = updated == errSecItemNotFound
                ? SecItemAdd(keychain.merging(attributes) { _, new in new } as CFDictionary, nil) : updated
        } else {
            status = SecItemDelete(keychain as CFDictionary)
        }
        let ok = status == errSecSuccess || (value == nil && status == errSecItemNotFound)
        return IosHostResult(value: nil, error: ok ? nil : "Could not update the protected session.")
    }

    func verify(token: String, issuer: String, audience: String, nonce: String?, reply: IosHostReply) {
        Task {
            do {
                let subject = try await verifiedSubject(token, issuer: issuer, audience: audience, nonce: nonce)
                reply.complete(value: subject, error: nil)
            } catch {
                reply.complete(value: nil, error: "Identity token validation failed.")
            }
        }
    }

    private func verifiedSubject(_ token: String, issuer: String, audience: String, nonce: String?) async throws -> String {
        let parts = token.split(separator: ".", omittingEmptySubsequences: false)
        guard issuer == config.issuer, audience == config.clientID, parts.count == 3, token.utf8.count <= 32_768,
              let headerData = Data(base64URL: parts[0]), let claimsData = Data(base64URL: parts[1]),
              let signature = Data(base64URL: parts[2]) else { throw PlatformError.invalidIdentityToken }
        let decoder = JSONDecoder()
        let header = try decoder.decode(JWTHeader.self, from: headerData)
        guard header.alg == "RS256", !header.kid.isEmpty, header.crit == nil,
              header.typ?.lowercased() != "at+jwt" else { throw PlatformError.invalidIdentityToken }
        let (data, response) = try await network.data(from: config.jwks)
        guard (response as? HTTPURLResponse)?.statusCode == 200, data.count <= 256_000 else {
            throw PlatformError.invalidIdentityToken
        }
        let matches = try decoder.decode(JWKSet.self, from: data).keys.filter { $0.kid == header.kid }
        guard matches.count == 1, let jwk = matches.first,
              let key = jwk.rsaVerificationKey(),
              SecKeyVerifySignature(key, .rsaSignatureMessagePKCS1v15SHA256,
                                    Data((parts[0] + "." + parts[1]).utf8) as CFData,
                                    signature as CFData, nil) else { throw PlatformError.invalidIdentityToken }
        let claims = try decoder.decode(IDClaims.self, from: claimsData)
        let now = Date().timeIntervalSince1970
        guard claims.iss == issuer, claims.aud.contains(audience), !claims.sub.isEmpty,
              claims.exp > now, claims.iat <= now + 60, claims.nbf.map({ $0 <= now }) ?? true,
              claims.aud.count == 1 || claims.azp == audience, claims.azp == nil || claims.azp == audience,
              claims.token_use != "access", nonce == nil || claims.nonce == nonce else {
            throw PlatformError.invalidIdentityToken
        }
        return claims.sub
    }
}

private struct JWTHeader: Decodable {
    let alg: String
    let kid: String
    let typ: String?
    let crit: [String]?
}

private struct IDClaims: Decodable {
    let iss: String
    let sub: String
    let aud: [String]
    let exp: Double
    let iat: Double
    let nbf: Double?
    let azp: String?
    let nonce: String?
    let token_use: String?

    init(from decoder: Decoder) throws {
        enum Key: String, CodingKey { case iss, sub, aud, exp, iat, nbf, azp, nonce, token_use }
        let c = try decoder.container(keyedBy: Key.self)
        iss = try c.decode(String.self, forKey: .iss)
        sub = try c.decode(String.self, forKey: .sub)
        if let audiences = try? c.decode([String].self, forKey: .aud) { aud = audiences }
        else { aud = [try c.decode(String.self, forKey: .aud)] }
        exp = try c.decode(Double.self, forKey: .exp)
        iat = try c.decode(Double.self, forKey: .iat)
        nbf = try c.decodeIfPresent(Double.self, forKey: .nbf)
        azp = try c.decodeIfPresent(String.self, forKey: .azp)
        nonce = try c.decodeIfPresent(String.self, forKey: .nonce)
        token_use = try c.decodeIfPresent(String.self, forKey: .token_use)
    }
}

private struct JWKSet: Decodable { let keys: [JWK] }

private struct JWK: Decodable {
    let kid: String?
    let kty: String
    let alg: String?
    let use: String?
    let key_ops: [String]?
    let n: String?
    let e: String?

    func rsaVerificationKey() -> SecKey? {
        guard kty == "RSA", alg == nil || alg == "RS256", use == nil || use == "sig",
              key_ops?.contains("verify") ?? true, let n, let e,
              let modulus = Data(base64URL: n[...]), let exponent = Data(base64URL: e[...]),
              (256...1024).contains(modulus.count), (1...8).contains(exponent.count) else { return nil }
        let body = Self.integer(modulus) + Self.integer(exponent)
        let der = Self.tlv(0x30, body)
        let attributes = [kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                          kSecAttrKeyClass as String: kSecAttrKeyClassPublic] as CFDictionary
        guard let key = SecKeyCreateWithData(der as CFData, attributes, nil),
              SecKeyGetBlockSize(key) >= 256 else { return nil }
        return key
    }

    private static func integer(_ value: Data) -> Data {
        var bytes = Data(value.drop(while: { $0 == 0 }))
        if bytes.first.map({ $0 & 0x80 != 0 }) ?? true { bytes.insert(0, at: 0) }
        return tlv(0x02, bytes)
    }

    private static func tlv(_ tag: UInt8, _ value: Data) -> Data {
        var length = value.count
        var bytes: [UInt8] = []
        if length < 128 { bytes = [UInt8(length)] }
        else {
            while length > 0 { bytes.insert(UInt8(length & 0xff), at: 0); length >>= 8 }
            bytes.insert(0x80 | UInt8(bytes.count), at: 0)
        }
        return Data([tag] + bytes) + value
    }
}

private extension Data {
    var base64URL: String {
        base64EncodedString().replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
    }

    init?(base64URL: Substring) {
        guard !base64URL.isEmpty, base64URL.utf8.allSatisfy({
            (65...90).contains($0) || (97...122).contains($0) || (48...57).contains($0) || $0 == 45 || $0 == 95
        }) else { return nil }
        let value = base64URL.replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        self.init(base64Encoded: value + String(repeating: "=", count: (4 - value.count % 4) % 4))
    }
}
