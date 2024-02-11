import hashlib

def calculate_sha256(data, opt=1):
  # Convert data to bytes if it’s not already
  if isinstance(data, str):
    data = data.encode('utf_8')
      
    if opt == 0:
      # Calculate SHA-256 hash (digest)
      sha256_hash = hashlib.sha256(data).digest()
    else:
      # Calculate SHA-256 hash (hexdigest)
      sha256_hash = hashlib.sha256(data).hexdigest()
    
    return sha256_hash

# Example usage:
if __name__ == '__main__':
  #input_data = "Hello, World!"
  input_data = "qortal"
  #hash_value = calculate_sha256(input_data)
  print("SHA-256 Hash (digest):", calculate_sha256(input_data,0))
  print("SHA-256 Hash (hexdigest):", calculate_sha256(input_data,1))
  print("")

  print("directly using hashlib:")
  h = hashlib.new('sha256')
  h.update(b"qortal")
  print("SHA-256 Hash (digest):", h.digest())
  print("SHA-256 Hash (hexdigest):", h.hexdigest())
  print("SHA-256 Hash (list):", list(h.digest()))
