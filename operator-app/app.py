#!/usr/bin/python3
# -*- coding: utf-8 -*-

# sudo apt-get install python3-pyqt5 pyqt5-dev-tools sip-dev python3-pip
# sudo pip3 install requests

import sys
from PyQt5.QtWidgets import (QApplication, QWidget, QPushButton, QGridLayout,
 QVBoxLayout, QHBoxLayout, QMessageBox, QDesktopWidget, QLineEdit, QLabel)
import json
import requests


class Example(QWidget):
    
    def __init__(self):
        super().__init__()
        
        self.initUI()        
        
    def initUI(self):        
        self.setGeometry(300, 300, 300, 220)
        self.setWindowTitle('Operator app')
        self.center()    

        mainLayout    = QGridLayout(self)

        self.ipEdit    = QLineEdit('http://127.0.0.1:8080')
        ipTitle   = QLabel('IP:')
        mainLayout.addWidget(ipTitle      , 0, 0)
        mainLayout.addWidget(self.ipEdit  , 0, 1, 1, 3)
        self.uuidEdit  = QLineEdit()
        uuidTitle = QLabel('UUID:')
        mainLayout.addWidget(uuidTitle    , 1, 0)
        mainLayout.addWidget(self.uuidEdit, 1, 1, 1, 3)

        frwdBtn   = QPushButton('Forward', self)
        leftBtn   = QPushButton('Left', self)
        bkwdBtn   = QPushButton('Backward', self)
        rightBtn  = QPushButton('Right', self)
        mainLayout.addWidget(frwdBtn, 3, 2)
        mainLayout.addWidget(leftBtn, 4, 1)
        mainLayout.addWidget(bkwdBtn, 4, 2)
        mainLayout.addWidget(rightBtn, 4, 3)
        frwdBtn.clicked.connect(self.buttonClicked)
        leftBtn.clicked.connect(self.buttonClicked)
        bkwdBtn.clicked.connect(self.buttonClicked)
        rightBtn.clicked.connect(self.buttonClicked)

        self.show()

    def sendCommand(self, command):
        data = {'clientUID': self.uuidEdit.text(), 'command': command, 'args':[]}
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        req = requests.post(self.ipEdit.text() + '/command', data=json.dumps(data), headers=headers)

    def buttonClicked(self):
        sender = self.sender()
        self.sendCommand(sender.text())

    def closeEvent(self, event):        
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
