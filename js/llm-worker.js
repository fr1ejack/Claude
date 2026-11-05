// Läuft in einem eigenen Thread, damit die 3D-Animation flüssig bleibt, während das Sprachmodell rechnet.
import { WebWorkerMLCEngineHandler } from 'https://cdn.jsdelivr.net/npm/@mlc-ai/web-llm@0.2.84/+esm';

const handler = new WebWorkerMLCEngineHandler();
self.onmessage = msg => handler.onmessage(msg);
