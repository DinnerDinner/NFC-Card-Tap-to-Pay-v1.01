"""

To restart the container (if already created before):
docker start nfc-postgres


To start the server local:
python -m venv env
env\Scripts\activate
uvicorn nfc_backend_fastapi.main:app --host 127.0.0.1 --port 8000

To start the LocalTunnel (replacing ngrok): 
lt --port 8000 --subdomain nfcwalletdev  # Try reusing this every time



IN CASE LOCAL TUNNEL NU BUENO -- To start the ngrok:
cd into C: only
ngrok http --url=promoted-quetzal-visually.ngrok-free.app 8000  

Take the ngrok url and plug into .url() in MainActivity.kt in Android Studio app and reBuild


######NO NEED FOR DOCKER ANYMORE SINCE SUPABASE INTEGRATION COMPLETE
To create the Docker PostgreSQL container with persistent volume:
docker volume create nfcwallet_data
docker run --name nfc-postgres -e POSTGRES_USER=nfcuser -e POSTGRES_PASSWORD=nfcpass -e POSTGRES_DB=nfcwallet -v nfcwallet_data:/var/lib/postgresql/data -p 5432:5432 -d postgres

To stop the container (e.g., before shutdown):
docker stop nfc-postgres

To check if it's running:
docker ps
"""