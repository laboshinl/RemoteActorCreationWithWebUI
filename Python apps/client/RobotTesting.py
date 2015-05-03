#!/usr/bin/python
import threading
import json
from ClientAPI import ClientAPI

__author__ = 'baka'

class Robot(object):
    def __init__(self, url):
        self.clientApi = ClientAPI(url, 'CommandProxy')
        self.start_remote_command_listener()


    def start_remote_command_listener(self):
        recv_command_socket = self.clientApi.get_recv_sock_with_topic('command')
        def run():
            print threading.current_thread().name, 'started...'
            while True:
                frames = recv_command_socket.recv_multipart()
                self.dispatch_command(frames[-1])
        threading.Thread(name='CommandThread', target=run).start()

    def dispatch_command(self, jsonStr):
        kvJson = json.loads(jsonStr)
        print kvJson['command'], kvJson['args']

    def disconnect(self):
        self.clientApi.disconnect()

    def get_id(self):
        return self.clientApi.client_id

def main():
    robot = Robot('http://192.168.1.6:8080')



if __name__ == "__main__":
    main()
