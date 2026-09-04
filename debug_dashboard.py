import re
import urllib.request
import urllib.parse
import http.cookiejar

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

login_html = opener.open('http://localhost:8081/login').read().decode('utf-8')
print('LOGIN PAGE LENGTH', len(login_html))
match = re.search(r'name="_csrf" value="([^"]+)"', login_html)
if not match:
    raise SystemExit('CSRF token not found')
token = match.group(1)
print('CSRF', token)

login_data = urllib.parse.urlencode({
    'username': 'admin@URBELIX.com',
    'password': 'admin123',
    '_csrf': token
}).encode('utf-8')
req = urllib.request.Request('http://localhost:8081/login', data=login_data)
req.add_header('Content-Type', 'application/x-www-form-urlencoded')
resp = opener.open(req)
print('LOGIN STATUS', resp.status, resp.geturl())

resp = opener.open('http://localhost:8081/dashboard')
print('DASHBOARD STATUS', resp.status)
content = resp.read().decode('utf-8', errors='replace')
print(content[:2000])

