#!/usr/bin/python
import threading
from client.ClientAPI import ClientAPI

__author__ = 'baka'

class Robot(object):
    def __init__(self, url):
        self.clientApi = ClientAPI(url, 'CommandProxy')
        self.startRemoteCommandListner()

    def startRemoteCommandListner(self):
        recv_command_socket = self.clientApi.get_recv_sock_with_topic('command')
        def run():
            print threading.current_thread().name, 'started...'
            while True:
                frames = recv_command_socket.recv_multipart()
                for str in frames:
                    print str
        threading.Thread(name='CommandThread', target=run).start()

    def disconnect(self):
        self.clientApi.disconnect()

    def get_id(self):
        return self.clientApi.client_id

def main():
    robot = Robot('http://127.0.0.1:8080')
    print robot.get_id()



if __name__ == "__main__":
    main()