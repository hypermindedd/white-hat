"""Exercise revision and identity handling across real HTTP multi-turn requests."""
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
from pathlib import Path
from tempfile import TemporaryDirectory
from threading import Thread
from types import SimpleNamespace
import unittest
from unittest.mock import patch
from uuid import uuid4

from run_s3 import run


class MultiTurnCollectionTest(unittest.TestCase):
    def test_later_turn_updates_the_same_decision_at_the_previous_revision(self):
        workspace, decision = str(uuid4()), str(uuid4())
        requests = []
        runs = {}

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def do_POST(self):
                self.respond()

            def do_PUT(self):
                self.respond()

            def do_GET(self):
                self.respond()

            def respond(self):
                body = json.loads(self.rfile.read(int(self.headers['Content-Length']))) if self.headers.get('Content-Length') else None
                requests.append((self.command, self.path, body, self.headers.get('Idempotency-Key')))
                if self.path.endswith('/runs'):
                    run_id = str(uuid4())
                    runs[run_id] = body['expected_revision']
                    response = {'id': run_id}
                elif '/runs/' in self.path:
                    run_id = self.path.rsplit('/', 1)[1]
                    response = {'id': run_id, 'status': 'succeeded', 'workspace_id': workspace,
                                'decision_id': decision, 'input_revision': runs[run_id], 'result': {}}
                else:
                    response = {'id': decision, 'revision': 1 if self.command == 'POST' else body['expected_revision'] + 1}
                data = json.dumps(response).encode()
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)

        server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        thread = Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with TemporaryDirectory() as directory, patch.dict(os.environ, {'WH_S3_ACCESS_TOKEN': uuid4().hex}):
                root = Path(directory)
                manifest = root / 'manifest.json'
                manifest.write_text('{}')
                args = SimpleNamespace(origin=f'http://127.0.0.1:{server.server_port}', workspace=workspace,
                                       out=root / 'receipts', manifest=manifest, execution_sha='a' * 40,
                                       model='fixture-only', max_submissions=2, poll_timeout=5)
                run([{'id': 'G001', 'turns': [{'decision': 'first'}, {'decision': 'second'}]}], args)
                updates = [row for row in requests if row[0] == 'PUT']
                self.assertEqual(updates[0][1], f'/api/v1/workspaces/{workspace}/decisions/{decision}')
                self.assertEqual(updates[0][2]['expected_revision'], 1)
                submissions = [row for row in requests if row[1].endswith('/runs')]
                self.assertEqual([row[2]['expected_revision'] for row in submissions], [1, 2])
                self.assertEqual(len({row[3] for row in submissions}), 2)
                receipt = json.loads((args.out / 'G001-2.json').read_text())
                self.assertEqual(receipt['revision'], 2)
                self.assertEqual(receipt['decision_id'], decision)
        finally:
            server.shutdown()
            server.server_close()
            thread.join()
