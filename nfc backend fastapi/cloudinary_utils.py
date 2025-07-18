import cloudinary
import cloudinary.uploader
import cloudinary.api

cloudinary.config(
    cloud_name = "dqhr9pgl0",
    api_key = "289124826863297",
    api_secret = "ZscnuDZzhq4uYAA2_qH6SIDTHkg"
)

def upload_image_to_cloudinary(file):
    result = cloudinary.uploader.upload(file.file)
    return result["secure_url"]
