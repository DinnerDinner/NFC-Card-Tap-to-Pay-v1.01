import re

def clean_phone_number(phone_number: str) -> str:
    return re.sub(r'\D', '', phone_number)

# Example usage:
print(clean_phone_number("(123) 456-7890"))  # Output: 1234567890
print(clean_phone_number("+1 800 555 1212")) # Output: 18005551212
print(clean_phone_number("abc123def456"))    # Output: 123456
