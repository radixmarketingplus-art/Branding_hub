import xml.etree.ElementTree as ET
import collections
import os

def check_folder(folder_path):
    all_names = []
    for filename in os.listdir(folder_path):
        if filename.endswith('.xml'):
            file_path = os.path.join(folder_path, filename)
            try:
                tree = ET.parse(file_path)
                root = tree.getroot()
                # Check strings
                for item in root.findall('string'):
                    all_names.append(item.get('name'))
                # Check string-arrays
                for item in root.findall('string-array'):
                    all_names.append(item.get('name'))
            except Exception as e:
                print(f"Error parsing {filename}: {e}")
    
    duplicates = [item for item, count in collections.Counter(all_names).items() if count > 1]
    return duplicates

res_path = r'C:\Users\user\AndroidStudioProjects\RMPlus\RMPlus\app\src\main\res'
print("values folder duplicates:", check_folder(os.path.join(res_path, 'values')))
print("values-hi folder duplicates:", check_folder(os.path.join(res_path, 'values-hi')))
