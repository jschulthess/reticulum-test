import hashlib
from Cryptography import HKDF

ifac_size = 16
ifac_key = [254, 245, 3, 113, 13, 58, 145, 202, 163, 27, 94, 100, 95, 247, 103, 130, 169, 102, 104, 252, 114, 33, 147, 9, 182, 67, 37, 30, 191, 210, 28, 157, 65, 215, 182, 18, 89, 88, 173, 111, 150, 167, 133, 188, 221, 79, 226, 142, 234, 108, 69, 182, 181, 129, 232, 25, 148, 47, 157, 133, 151, 77, 88, 112]
#ifac_key = b'\xfe\xf5\x03q\r:\x91\xca\xa3\x1b^d_\xf7g\x82\xa9fh\xfcr!\x93\t\xb6C%\x1e\xbf\xd2\x1c\x9dA\xd7\xb6\x12YX\xado\x96\xa7\x85\xbc\xddO\xe2\x8e\xealE\xb6\xb5\x81\xe8\x19\x94/\x9d\x85\x97MXp'
#ifac_key = list(ifac_key) = [254, 245, 3, 113, 13, 58, 145, 202, 163, 27, 94, 100, 95, 247, 103, 130, 169, 102, 104, 252, 114, 33, 147, 9, 182, 67, 37, 30, 191, 210, 28, 157, 65, 215, 182, 18, 89, 88, 173, 111, 150, 167, 133, 188, 221, 79, 226, 142, 234, 108, 69, 182, 181, 129, 232, 25, 148, 47, 157, 133, 151, 77, 88, 112]
raw = [-94, 91, 66, -32, -8, -75, -112, -94, -103, -111, 54, -49, 120, -13, -105, 10, 105, 3, -62, 23, -63, 7, -31, 94, -116, -73, 79, -99, -101, -104, 85, 57, -121, 87, 88, 104, -64, 117, 102, -27, 125, -112, -108, -43, -31, 15, 15, -69, 11, 29, 101, 6, -23, 68, -90, -7, 63, -117, 53, -23, 37, -127, -41, -115, -126, -63, -119, 10, -49, -36, -8, -80, -86, -65, 50, 60, 67, -66, -78, 10, 109, -59, 114, 40, 93, -94, -124, 21, -108, 115, -51, 25, -118, 106, -114, -100, -100, -62, -51, -63, 115, 74, -65, -50, 8, -52, -14, -65, -21, 17, 15, -91, 9, 85, -74, -124, -42, 99, 109, 82, 98, 124, 100, 8, -47, 89, 65, -102, -128, -57, -41, -10, -122, -113, 79, -54, 16, -22, 11, -54, 121, 4, 37, 81, -4, 52, 71, -66, 59, -61, -74, -1, -39, 4, -76, -45, -45, -109, -51, 42, 34, -19, -81, -10, 91, 14, -68, -39, -52, -29, -31, -110, -69, 61, -5, -45, -7, 107, 31, 123, 29, -123, 92, 38, -55, -101, 64, 90, 15, 104, 20, -105, 107, 119, -115, 41, 66, -78, -8, 0, -11, 44, 87, 15]

def signed_to_unsigned(signed):
    """ conert a list of signed integers to a list of unsigned integers """
    result = []
    for i in signed:
        if i < 0:
            result.append(i+255)
        else:
            result.append(i)
    return result

if __name__ == '__main__':
    # Extract IFAC
    ifac = raw[2:2+ifac_size]
    print("ifac: "+str(ifac))
    
    print(signed_to_unsigned(ifac))

    # Generate mask
    mask = HKDF.hkdf(
        length=len(raw),
        derive_from=bytes(signed_to_unsigned(ifac)),
        salt=bytes(ifac_key),
        context=None,
    )
    print("* mask: "+str(list(mask))+" length: "+str(len(mask)))
    
    # Unmask payload
    i = 0; unmasked_raw = b""
    for byte in signed_to_unsigned(raw):
        if i <= 1 or i > ifac_size+1:
            # Unmask header bytes and payload
            unmasked_raw += bytes([byte ^ mask[i]])
        else:
            # Don't unmask IFAC itself
            unmasked_raw += bytes([byte])
        i += 1
    raw = unmasked_raw
    print("* raw after unmask: "+str(list(raw)))

    # Unset IFAC flag
    new_header = bytes([raw[0] & 0x7f, raw[1]])
    print("* new_header after unset IFAC flag: "+str(new_header))
    
    # Re-assemble packet
    new_raw = new_header+raw[2+ifac_size:]
    print("* raw after unmask: "+str(list(new_raw)))
    