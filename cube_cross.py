import sys, os
from PIL import Image, ImageDraw, ImageFont

TAG = sys.argv[1] if len(sys.argv) > 1 else 'garage'
SIZE = 512
PAD = 48
COLS, ROWS = 4, 3

# bgfx / D3D cube face order. Placed UNROTATED on purpose: the request is to
# show the actual texture data, not a tidied-up projection.
#   0 +X Right, 1 -X Left, 2 +Y Top, 3 -Y Bottom, 4 +Z Front, 5 -Z Back
POS = {
    2: (1, 0),
    1: (0, 1), 4: (1, 1), 0: (2, 1), 5: (3, 1),
    3: (1, 2),
}
NAME = {
    0: 'face 0  +X  Right',
    1: 'face 1  -X  Left',
    2: 'face 2  +Y  Top',
    3: 'face 3  -Y  Bottom',
    4: 'face 4  +Z  Front',
    5: 'face 5  -Z  Back',
}

def font(sz):
    try:
        return ImageFont.truetype('arial.ttf', sz)
    except Exception:
        return ImageFont.load_default()

for cube in (0, 1):
    W = COLS * SIZE + (COLS + 1) * PAD
    H = ROWS * SIZE + (ROWS + 1) * PAD
    img = Image.new('RGB', (W, H), (28, 28, 32))
    d = ImageDraw.Draw(img)

    for face, (cx, cy) in POS.items():
        path = 'E:/OpenSLR/dump_%s_cube%d_face%d.raw' % (TAG, cube, face)
        if not os.path.exists(path):
            continue
        raw = open(path, 'rb').read()
        f = Image.frombytes('RGBA', (SIZE, SIZE), raw).convert('RGB')
        x = PAD + cx * (SIZE + PAD)
        y = PAD + cy * (SIZE + PAD)
        img.paste(f, (x, y))
        d.rectangle([x, y, x + SIZE, y + SIZE], outline=(255, 255, 255), width=2)

        # Up and Right arrows, so the orientation of the raw data is explicit.
        ax, ay = x + 24, y + SIZE - 24
        d.line([ax, ay, ax, ay - 60], fill=(255, 60, 60), width=5)
        d.polygon([(ax, ay - 74), (ax - 11, ay - 54), (ax + 11, ay - 54)], fill=(255, 60, 60))
        d.text((ax - 6, ay - 104), 'Up', fill=(255, 90, 90), font=font(24))
        d.line([ax, ay, ax + 60, ay], fill=(60, 160, 255), width=5)
        d.polygon([(ax + 74, ay), (ax + 54, ay - 11), (ax + 54, ay + 11)], fill=(60, 160, 255))
        d.text((ax + 78, ay - 14), 'Right', fill=(90, 180, 255), font=font(24))

        d.text((x + 12, y + 10), NAME[face], fill=(255, 255, 120), font=font(26))

    d.text((PAD, 12),
           'cube%d  (read index %d / write index %d)  tag=%s  512x512 RGBA8, faces unrotated'
           % (cube, 1, 0, TAG),
           fill=(255, 255, 255), font=font(26))
    out = 'E:/OpenSLR/cube%d_cross.png' % cube
    img.save(out)
    print('wrote', out)

# raw faces, untouched, for side-by-side checking without any transform
for cube in (0, 1):
    row = []
    for face in range(6):
        p = 'E:/OpenSLR/dump_%s_cube%d_face%d.raw' % (TAG, cube, face)
        row.append(Image.frombytes('RGBA', (SIZE, SIZE), open(p, 'rb').read()).convert('RGB'))
    sheet = Image.new('RGB', (SIZE * 6 + 5 * 8, SIZE + 40), (20, 20, 20))
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(row):
        sheet.paste(f, (i * (SIZE + 8), 36))
        d.text((i * (SIZE + 8) + 6, 8), NAME[i], fill=(255, 255, 120), font=font(22))
    out = 'E:/OpenSLR/cube%d_faces_raw.png' % cube
    sheet.save(out)
    print('wrote', out)
