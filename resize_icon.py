import os
from PIL import Image

logo_path = r"C:\Users\Administrator\.gemini\antigravity\brain\fb611aa1-28f7-4359-bdfa-d79d9d157a07\kds_app_logo_1771662190363.png"
res_dir = r"c:\Users\Administrator\Desktop\Food POS\android\app\src\main\res"

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

try:
    with Image.open(logo_path) as img:
        img = img.convert("RGBA")
        for density, size in sizes.items():
            mipmap_dir = os.path.join(res_dir, f"mipmap-{density}")
            if not os.path.exists(mipmap_dir):
                os.makedirs(mipmap_dir)
            
            # Resize
            resized = img.resize((size, size), Image.Resampling.LANCZOS)
            
            # Save normal
            resized.save(os.path.join(mipmap_dir, "ic_launcher.png"), "PNG")
            
            # Save round
            # We will just use the same image but theoretically could mask to circle
            resized.save(os.path.join(mipmap_dir, "ic_launcher_round.png"), "PNG")
            
    print("SUCCESS: Icons resized and saved.")
except Exception as e:
    print(f"FAILED: {e}")
