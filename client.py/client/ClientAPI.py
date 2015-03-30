#!/usr/bin/python

__author__ = 'baka'

import zmq
import requests
import json
import time


class ClientAPI(object):
    def __init__(self, url, actor_type):
        self.url = url
        data = {'actorType': actor_type}
        self.headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        req = requests.put(self.url + '/actor', data=json.dumps(data), headers=self.headers)
        if req.json()['Status'] == 'Success':
            task_id = req.json()['TaskId']
        else:
            raise Exception("""Can't register task""")
        resp = self.wait_for_task(task_id)
        if resp.json()['Status'] == "Success":
            [client_id, sub_string, send_string] = \
                [resp.json()['clientUID'], resp.json()['subString'], resp.json()['sendString']]
        else:
            raise Exception("""Can't execute task with reason: """ + resp.json()['Result'])
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
        status = 'Incomplete'
        while status == 'Incomplete':
            url = self.url + '/task'
            data = {'Id': task_id}
            req = requests.get(url, data=json.dumps(data), headers=self.headers)
            status = req.json()['Status']
        if req.json()['Status'] == 'Error':
            raise Exception("""Error on task with id: """ + task_id + " with reason: " + req.json()['Result'])
        return req

    def get_recv_sock_with_topic(self, topic):
        recv_socket = self.context.socket(zmq.SUB)
        recv_socket.connect(self.sub_string)
        recv_socket.setsockopt(zmq.SUBSCRIBE, self.client_id + '.' + topic)
        return recv_socket

    def try_send(self, msg):
        print 'Sending msg: %s' % msg
        frames = [self.client_id.encode() + '.ololo'.encode(), msg.encode()]
        print 'Create and send frames: ', frames
        self.send_socket.send_multipart(msg_parts=frames)

    def try_recv(self):
        print 'Receiving msg...'
        frames = self.recv_socket.recv_multipart()
        print 'Received frames: ', frames

    def disconnect(self):
        print 'Disconnecting...'
        data = {'Id': self.client_id}
        req = requests.delete(self.url + '/actor', data=json.dumps(data), headers=self.headers)
        if req.json()['Status'] == 'Success':
            task_id = req.json()['TaskId']
        else:
            raise Exception("""Can't register task""")
        resp = self.wait_for_task(task_id)
        print 'Response: ', resp.json()["Result"] == self.client_id

def main():
    client_api = ClientAPI('http://127.0.0.1:8080', 'ParrotActor')
    for i in range(1, 2):
        client_api.try_send('ololo %s' % str(i))
        client_api.try_recv()

    for i in range(1, 2):
        client_api.try_send('ololo %s' % str(i))

    print '\nSleeping...\n'
    time.sleep(2)

    for i in range(1, 2):
        client_api.try_recv()

    client_api.disconnect()

if __name__ == "__main__":
    main()
