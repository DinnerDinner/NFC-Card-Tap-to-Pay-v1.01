import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))
from db_files.database import SessionLocal
from db_files.models import User

def print_all_users_with_card_uid():
    session = SessionLocal()
    try:
        users = session.query(User).filter(User.card_uid != None).all()
        
        if not users:
            print("No users with card_uid found in database.")
            return
        
        for user in users:
            print(f"ID: {user.id} | Name: {user.first_name} {user.last_name} | "
                  f"Email: {user.email} | Phone: {user.phone_number} | DOB: {user.dob} | "
                  f"Card UID: {user.card_uid} | Balance (cents): {user.balance_cents} | "
                  f"Created at: {user.created_at} | Last login: {user.last_login} | "
                  f"Is Cadet: {user.is_cadet} | Is Hospital User: {user.is_hospital_user} | Is Student: {user.is_student} | "
                  f"Status: {user.status} | Password: {user.password_hash}")
    finally:
        session.close()

if __name__ == "__main__":
    print_all_users_with_card_uid()
