#!/usr/bin/python
import json
from ClientAPI import ClientAPI
from rpi_serial import *

__author__ = 'baka'

class Robot(object):
    def __init__(self, url):
        self.clientApi = ClientAPI(url, 'CommandProxy')
        self.start_remote_command_listener()
        self.command_to_arduino = {
            'Forward': lambda json: (doMotorRun('M1', int(json['args'][0])), doMotorRun('M2', int(json['args'][0]))),
            'Right': lambda json: (doMotorRun('M1', int(json['args'][0])), doMotorRun('M2', -int(json['args'][0]))),
            'Left': lambda json: (doMotorRun('M1', -int(json['args'][0])), doMotorRun('M2', int(json['args'][0]))),
            'Backward': lambda json: (doMotorRun('M1', -int(json['args'][0])), doMotorRun('M2', -int(json['args'][0])))
        }

    def start_remote_command_listener(self):
        recv_command_socket = self.clientApi.get_recv_sock_with_topic('command')
        def run():
            print threading.current_thread().name, 'started...'
            while True:
                frames = recv_command_socket.recv_multipart()
                self.dispatch_command(frames[-1])
        th = threading.Thread(name='CommandThread', target=run)
        th.setDaemon(True)
        th.start()

    def dispatch_command(self, json_str):
        kv_json = json.loads(json_str)
        self.command_to_arduino[kv_json['command']](kv_json)

    def disconnect(self):
        self.clientApi.disconnect()

    def get_id(self):
        return self.clientApi.client_id

def main():
    robot = Robot('http://192.168.1.6:8080')
    while True:
        cmd = raw_input("Input motor speed\n")
        try:
            speed = int(cmd)
            doMotorRun("M1", speed)
            doMotorRun("M2", speed)
        except ValueError:
            print "please input a number"




if __name__ == "__main__":
    main()