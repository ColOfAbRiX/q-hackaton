
import os
import json

def query_elastic(path):
    sample_data = os.path.join(os.path.abspath(os.path.dirname(__file__)), "sample_data.json")
    with open(sample_data) as f:
        data = json.load(f)
    return data
