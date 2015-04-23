#!/usr/bin/python
import threading
from client.ClientAPI import ClientAPI
import serial

__author__ = 'baka'

serial = serial.Serial('/dev/ttyACM0', 19200, timeout=1)

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

    def dispatch_command(self, json):
        print json
        # self.send_command(0, [4])

    def send_command(self, command_code, command_args):
        first = '\x15'
        second = '\x90'
        maxCount = bytes(bytearray([len(command_args) + 1]))
        serial.write(first + second + maxCount + bytearray([command_code]) + bytes(bytearray(command_args)))

    def disconnect(self):
        self.clientApi.disconnect()

    def get_id(self):
        return self.clientApi.client_id

def main():
    robot = Robot('http://195.208.117.228:8080')



if __name__ == "__main__":
    main()