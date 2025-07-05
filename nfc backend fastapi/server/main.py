import random
from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session

from db_files.database import SessionLocal, engine, Base
from db_files.models import Card

Base.metadata.create_all(bind=engine)
app = FastAPI()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_or_create_card(uid: str, db: Session) -> tuple[Card, bool]:
    card = db.query(Card).filter(Card.uid == uid).first()
    if card:
        return card, True                          
    # create new
    new_balance = round(random.uniform(50, 500), 2) # dummy starting money
    card = Card(uid=uid, balance=new_balance)
    db.add(card)
    db.commit()
    db.refresh(card)
    return card, False                              # new user


@app.post("/nfc-tap")
def nfc_tap(payload: dict, db: Session = Depends(get_db)):
    uid = payload.get("uid")
    card, existing = get_or_create_card(uid, db)

    return {
        "uid": card.uid,
        "balance": card.balance,
        "existing_user": existing,
        "message": (
            f"Card ending in {card.uid[-4:]} balance ${card.balance}"
            if existing
            else f"New card created, starting balance ${card.balance}"
        )
    }
