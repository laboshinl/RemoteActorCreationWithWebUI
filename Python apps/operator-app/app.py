#!/usr/bin/python3
# -*- coding: utf-8 -*-

# sudo apt-get install python3-pyqt5 pyqt5-dev-tools sip-dev python3-pip
# sudo pip3 install requests

import sys
from PyQt5.QtWidgets import (QApplication, QWidget, QPushButton, QGridLayout,
 QVBoxLayout, QHBoxLayout, QMessageBox, QDesktopWidget, QLineEdit, QLabel)
import json
import requests

# Денис, есть такая штука, как PEP-8. Позязя, позязя, позязя, юзай его, ну позязя...
# Иначе я тебя убью! Нагло и расчленённо!!! *devil_dance*
# А то у меня IDE весь код подчёркмвает со словами "ты ебанько???!"

class Example(QWidget):
    def __init__(self):
        super().__init__()
        self.ip_edit    = QLineEdit('http://127.0.0.1:8080')
        self.uuid_edit  = QLineEdit()
        self.init_ui()
        
    def init_ui(self):        
        self.setGeometry(300, 300, 300, 220)
        self.setWindowTitle('Operator app')
        self.center()    

        main_layout    = QGridLayout(self)
        ip_title   = QLabel('IP:')
        main_layout.addWidget(ip_title      , 0, 0)
        main_layout.addWidget(self.ip_edit  , 0, 1, 1, 3)
        uuid_title = QLabel('UUID:')
        main_layout.addWidget(uuid_title    , 1, 0)
        main_layout.addWidget(self.uuid_edit, 1, 1, 1, 3)
        forward_button   = QPushButton('Forward', self)
        left_button   = QPushButton('Left', self)
        backward_button   = QPushButton('Backward', self)
        right_button  = QPushButton('Right', self)
        main_layout.addWidget(forward_button, 3, 2)
        main_layout.addWidget(left_button, 4, 1)
        main_layout.addWidget(backward_button, 4, 2)
        main_layout.addWidget(right_button, 4, 3)
        forward_button.clicked.connect(self.button_clicked)
        left_button.clicked.connect(self.button_clicked)
        backward_button.clicked.connect(self.button_clicked)
        right_button.clicked.connect(self.button_clicked)
        self.show()

    def send_command(self, command):
        data = {'clientUID': self.uuid_edit.text(), 'command': command, 'args':[]}
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        requests.post(self.ip_edit.text() + '/command', data=json.dumps(data), headers=headers)

    def button_clicked(self):
        sender = self.sender()
        self.send_command(sender.text())

    def close_event(self, event):
        reply = QMessageBox.question(self, 'Message',
            "Are you sure to quit?", QMessageBox.Yes | 
            QMessageBox.No, QMessageBox.No)
        if reply == QMessageBox.Yes:
            event.accept()
        else:
            event.ignore() 

    def center(self):        
        qr = self.frameGeometry()
        cp = QDesktopWidget().availableGeometry().center()
        qr.moveCenter(cp)
        self.move(qr.topLeft())
        
        
if __name__ == '__main__':    
    app = QApplication(sys.argv)
    ex = Example()
    sys.exit(app.exec_())  
