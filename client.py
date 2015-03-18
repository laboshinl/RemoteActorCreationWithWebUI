#!/usr/bin/python

__author__ = 'baka'

import zmq
import requests
import json
import time


class ClientAPI(object):
    def __init__(self, url):
        self.url = url
        data = {'actorType': 'ParrotActor'}
        self.headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        req = requests.put(self.url + '/actor', data=json.dumps(data), headers=self.headers)
        task_id = req.text.split(': ')[1]
        resp = self.wait_for_task(task_id)
        [client_id, sub_string, send_string] = resp.split(' ')
        self.client_id = str(client_id)
        self.sub_string = sub_string
        self.send_string = send_string
        self.__create_zmq_sockets()
        print 'Connection created: %s with sub address: %s and send address: %s' % (self.client_id, self.sub_string, self.send_string)


    def __create_zmq_sockets(self):
        self.context = zmq.Context()
        self.send_socket = self.context.socket(zmq.DEALER)
        self.recv_socket = self.context.socket(zmq.SUB)
        self.recv_socket.connect(self.sub_string)
        self.recv_socket.setsockopt(zmq.SUBSCRIBE, self.client_id)
        self.send_socket.connect(self.send_string)

    def wait_for_task(self, task_id):
        resp = 'Task not ready yet'
        while resp == 'Task not ready yet':
            url = self.url + '/task'
            data = {'Id': task_id}
            req = requests.get(url, data=json.dumps(data), headers=self.headers)
            resp = req.text
        return resp

    def try_send(self, msg):
        print 'Sending msg: %s' % msg
        frames = [self.client_id.encode() + '.ololo'.encode(), msg.encode()]
        print 'Create and send frames: ', frames
        self.send_socket.send_multipart(msg_parts=frames)

    def try_recv(self):
        print 'Receiving msg...'
        frames = self.recv_socket.recv_multipart()
        print 'Received frames: ', frames

def main():
    client_api = ClientAPI('http://127.0.0.1:8080')
    for i in range(1, 100):
        client_api.try_send('ololo %s' % str(i))
        client_api.try_recv()

    for i in range(1, 100):
        client_api.try_send('ololo %s' % str(i))

    print '\nSleeping...\n'
    time.sleep(2)

    for i in range(1, 100):
        client_api.try_recv()

if __name__ == "__main__":
    main()

#TODO: it's highly recommended to write disconnect method!