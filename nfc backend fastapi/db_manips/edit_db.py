import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from db_files.database import SessionLocal
from db_files.models import Card

def update_balance(id:int, new_balance: float):
    db = SessionLocal()
    try:
        card = db.query(Card).filter(Card.id == id).first()
        if card:
            card.balance = new_balance
            db.commit()
            print(f"✅ Balance updated: {card.uid} -> ${new_balance}")
        else:
            print(f"❌ Card with UID {card.uid} not found.")
    finally:
        db.close()





def delete_card(ids:list[int]):
    db = SessionLocal()
    for id in ids:
        try:
            card = db.query(Card).filter(Card.id == id).first()
            if card:
                db.delete(card)
                db.commit()
                print(f"🗑️ Card with UID {card.uid} deleted from database.")
            else:
                print(f"❌ No card found.")
        finally:
            db.close()





if __name__ == "__main__":
    # update_balance(12, 210)
    delete_card([13])
