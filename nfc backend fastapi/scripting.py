"""
To start the server local:
python -m venv env
env\Scripts\activate
uvicorn main:app --host 127.0.0.1 --port 8000

To start the ngrok:
cd into C: only
ngrok https 8000

To start the docker container with db:
you can play it from docker desktop
and run docker ps in cmd to check if its up

"""