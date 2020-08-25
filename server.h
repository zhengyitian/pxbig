#ifndef MAINWINDOW_H
#define MAINWINDOW_H


#include <QMainWindow>
#include <QPaintEvent>

#include "ui_mainwindow.h"
#include "qtimer.h"
#include "qdebug.h"
#include "qdatetime.h"
#include "qpixmap.h"
#include "qscreen.h"
#include "QPaintEvent"
#include "qpainter.h"
#include "qcursor.h"
#include "QDesktopWidget"
#include "QSettings"

#include "QtNetwork/qtcpsocket.h"
#include "QHBoxLayout"
#include "qpushbutton.h"
#include "qlineedit.h"

#include "qtcpserver.h"

namespace Ui {
class MainWindow;
}


class MainWindow : public QWidget
{
    Q_OBJECT
public:
    QTcpServer* server = new QTcpServer(this);
    QTcpSocket *socket;
    QByteArray data;

    explicit MainWindow(QWidget *parent = nullptr)
    {

        server->listen(QHostAddress::Any, 8899);
        connect(server, SIGNAL(newConnection()), SLOT(newConnection()));
    }



    ~MainWindow(){}



public slots:
    void newConnection()
    {
        socket = server->nextPendingConnection();
        connect(socket, SIGNAL(readyRead()), SLOT(dealRead()));
    }




    void dealRead()
    {
        auto a = socket->readAll();
        int xs,ys,xl,yl;
        memcpy((char*)&xs,a.data(),4);
        memcpy((char*)&ys,a.data()+4,4);
        memcpy((char*)&xl,a.data()+8,4);
        memcpy((char*)&yl,a.data()+12,4);

        QScreen *screen  =  QGuiApplication::primaryScreen();
        auto im=screen->grabWindow(0,xs, ys, xl, yl).toImage();
        QByteArray d;
        for(int j=0;j<yl;j++)
        {
            for(int i=0;i<xl;i++)
            {
                auto argb = im.pixel(i,j);
                d.push_back(qRed(argb));
                d.push_back(qGreen(argb));
                d.push_back(qBlue(argb));
                d.push_back(88);

            }
        }
        qDebug()<<d.size();
        socket->write(d.data(),d.size());

        socket->waitForBytesWritten();
        socket->close();
        socket->deleteLater();
        socket = nullptr;
    }
};




#endif
