from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

app = FastAPI()
latest_uid = None

@app.post("/nfc-tap")
async def receive_nfc_data(request: Request):
    global latest_uid
    data = await request.json()
    uid = data.get("uid", None)
    latest_uid = uid
    print(f"✅ NFC UID Received: {uid}")
    formatted_msg = f"Card ending in {uid[-4:]} detected!!"

    return JSONResponse(content={"message": formatted_msg}, status_code=200)

@app.get("/latest-uid")
async def get_latest_uid():
    return {"latest_uid": latest_uid}
