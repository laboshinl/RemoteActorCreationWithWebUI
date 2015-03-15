#!/usr/bin/python

import json
import requests

data = {'actorType':'ParrotActor'}
url = 'http://127.0.0.1:8080/actor'
headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
req = requests.put(url, data=json.dumps(data), headers=headers)
task_id = req.text.split(': ')[1]

resp = 'Task not ready yet'
while resp == 'Task not ready yet':
	url = 'http://127.0.0.1:8080/task'
	data = {'Id' : task_id}
	req = requests.get(url, data=json.dumps(data), headers=headers)
	resp = req.text

[client_id, subString, senString] = resp.split(' ')

