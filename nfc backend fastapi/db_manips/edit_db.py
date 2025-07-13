import sys
import os
from datetime import datetime, date

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from db_files.database import SessionLocal
from db_files.models import User, Business

# Map column names to their types (simplified)
COLUMN_TYPES = {
    "first_name": str,
    "last_name": str,
    "email": str,
    "phone_number": str,
    "dob": date,
    "password_hash": str,
    "created_at": datetime,
    "last_login": datetime,
    "status": str,
    "balance_cents": int,
    "card_uid": str,
    "cadet_card_uid": str,
    "hospital_card_uid": str,
    "school_card_uid": str,
    "is_cadet": bool,
    "is_hospital_user": bool,
    "is_student": bool,
}

def cast_value(value: str, to_type):
    if to_type == bool:
        return value.strip().lower() in ("true", "1", "yes", "y")
    elif to_type == int:
        return int(value)
    elif to_type == datetime:
        # Try parse ISO format or common datetime formats
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
            try:
                return datetime.strptime(value, fmt)
            except ValueError:
                continue
        raise ValueError(f"Cannot parse datetime from '{value}'")
    elif to_type == date:
        # Parse date only
        for fmt in ("%Y-%m-%d",):
            try:
                return datetime.strptime(value, fmt).date()
            except ValueError:
                continue
        raise ValueError(f"Cannot parse date from '{value}'")
    else:
        return value

def update_user_field(user_id: int, field_name: str, new_value: str):
    session = SessionLocal()
    try:
        user = session.query(User).filter(User.id == user_id).first()
        if not user:
            print(f"❌ User with id {user_id} not found.")
            return
        
        if field_name not in COLUMN_TYPES:
            print(f"❌ Field '{field_name}' is not editable or does not exist.")
            return
        
        # Cast new_value to the correct type
        casted_value = cast_value(new_value, COLUMN_TYPES[field_name])

        setattr(user, field_name, casted_value)
        session.commit()
        print(f"✅ Updated user id {user_id}: set {field_name} = {casted_value}")
    except Exception as e:
        print(f"❌ Error updating user: {e}")
    finally:
        session.close()

def delete_users_by_ids(ids: list[int]):
    session = SessionLocal()
    try:
        for user_id in ids:
            user = session.query(User).filter(User.id == user_id).first()
            if user:
                session.delete(user)
                session.commit()
                print(f"🗑️ User with id {user_id} deleted.")
            else:
                print(f"❌ No user found with id {user_id}.")
    finally:
        session.close()

def interactive_update():
    print("Editable fields:")
    for col in COLUMN_TYPES.keys():
        print(f"- {col}")
    field = input("Enter the field you want to update: ").strip()
    if field not in COLUMN_TYPES:
        print("Invalid field name.")
        return

    user_id_str = input("Enter the user ID to update: ").strip()
    if not user_id_str.isdigit():
        print("Invalid user ID.")
        return
    user_id = int(user_id_str)

    new_value = input(f"Enter new value for '{field}': ").strip()

    update_user_field(user_id, field, new_value)

def interactive_delete():
    ids_str = input("Enter user IDs to delete (comma separated): ").strip()
    try:
        ids = [int(x.strip()) for x in ids_str.split(",") if x.strip().isdigit()]
        if not ids:
            print("No valid IDs entered.")
            return
        delete_users_by_ids(ids)
    except Exception as e:
        print(f"Error parsing IDs: {e}")



def delete_businesses_by_ids(ids: list[int]):
    session = SessionLocal()
    try:
        for business_id in ids:
            business = session.query(Business).filter(Business.id == business_id).first()
            if business:
                session.delete(business)
                session.commit()
                print(f"🗑️ Business with id {business_id} deleted.")
            else:
                print(f"❌ No business found with id {business_id}.")
    except Exception as e:
        print(f"❌ Error deleting businesses: {e}")
    finally:
        session.close()

def interactive_delete_businesses():
    ids_str = input("Enter business IDs to delete (comma separated): ").strip()
    try:
        ids = [int(x.strip()) for x in ids_str.split(",") if x.strip().isdigit()]
        if not ids:
            print("No valid IDs entered.")
            return
        delete_businesses_by_ids(ids)
    except Exception as e:
        print(f"Error parsing IDs: {e}")

if __name__ == "__main__":
    interactive_delete_businesses()
    action = input("Choose action: [update/delete]: ").strip().lower()
    if action == "update":
        interactive_update()
    elif action == "delete":
        interactive_delete()
    else:
        print("Unknown action. Please type 'update' or 'delete'.")





