from flask import Flask, request, send_from_directory
import data

app = Flask("Hackaton")

@app.route('/')
def send_root():
    return send_html('index.html')

@app.route('/css/<path:path>')
def send_css(path):
    return send_from_directory('../frontend/css', path)

@app.route("/<path:path>")
def send_html(path):
    return send_from_directory('../frontend/html', path)

@app.route('/images/<path:path>')
def send_images(path):
    return send_from_directory('../frontend/images', path)

@app.route('/js/<path:path>')
def send_js(path):
    return send_from_directory('../frontend/lib', path)


@app.route("/data/<path>")
def query_elastic(path):
    return data.query_elastic(path)

if __name__ == "__main__":
  app.run()
