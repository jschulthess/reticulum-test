import hashlib

if __name__ == '__main__':
  data = "qortal"
  data = data.encode('utf_8')
  print("hash of value='qortal':")
  h = hashlib.new('sha256')
  #h.update(b"qortal")
  h.update(data)
  print("SHA-256 Hash (digest):", h.digest())
  print("SHA-256 Hash (hexdigest):", h.hexdigest())
  print("SHA-256 Hash (list):", list(h.digest()))
