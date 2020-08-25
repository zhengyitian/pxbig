#ifndef MAINWINDOW_H
#define MAINWINDOW_H


#include <QMainWindow>
#include <QPaintEvent>
#include <qt_windows.h>
#include "mainwindow.h"
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
#include "qnetwork.h"
#include "QtNetwork/qtcpsocket.h"
#include "QHBoxLayout"
#include "qpushbutton.h"
#include "qlineedit.h"



namespace Ui {
class MainWindow;
}

class myLine : public QLineEdit
{
    Q_OBJECT

signals:
    void wh();

public:
    explicit myLine(QString t=""):QLineEdit(t)
    {

    }
    virtual void wheelEvent(QWheelEvent *e)
    {
        int aa = e->angleDelta().y()/120;
        int t = aa+ text().toInt();
        setText(QString::number(t));
        emit(wh());
    }
};


class MainWindow : public QWidget
{
    Q_OBJECT
public:
    QTcpSocket* sock=nullptr;
    Ui::MainWindow *ui;
    int   xLen = 2244,
    yLen = 2244,
    xS = 800,
    yS = 800,
    yL = 50 ,
    xL = 50  ,
    xS2=xS,yS2=yS,xL2=xL,yL2=yL,
    drawP1 = 0,
    drawP2 = 50,
    maxX = 1366,
    maxY = 768;
    QByteArray data;
    QImage pic;
    bool pause = false;
    int reso = 0;
    myLine *l1,*l2,*l3,*l4,*l5;

    explicit MainWindow(QWidget *parent = nullptr)
    {

        QTimer *timer = new QTimer(this);
        connect(timer, SIGNAL(timeout()), this, SLOT(onT()));
        timer->start(50);
        QCoreApplication::instance()->installEventFilter(this);

        QVBoxLayout *v = new QVBoxLayout();
        QHBoxLayout *h = new QHBoxLayout();

        QPushButton *p = new QPushButton("set");
        connect(p, SIGNAL(clicked()), this, SLOT(onCli()));

        l1 = new myLine(QString::number(xS));
        l2 = new myLine(QString::number(yS));
        l3 = new myLine(QString::number(xL));
        l4 = new myLine(QString::number(yL));
        l5 = new myLine(QString::number(reso));
        connect(l1, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l2, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l3, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l4, SIGNAL(wh()), this, SLOT(onCli()));
        connect(l5, SIGNAL(wh()), this, SLOT(onCli()));

        h->addWidget(l1);
        h->addWidget(l2);
        h->addWidget(l3);
        h->addWidget(l4);
        h->addWidget(l5);
        h->addWidget(p);
        v->addLayout(h);
        v->addStretch();
        this->setLayout(v);
    }

    void drawOne(int ty,int v,int i,int j,QImage& im)
    {
        int   redCount = v*reso*reso/3/255;
        int   redCo = 0;
        int gap =  reso/3*ty;
        for (int ii=i*reso+gap;ii<gap+i*reso+reso/3;ii++)
        {
            for(int jj=j*reso;jj<(j+1)*reso;jj++)
            {
                if( redCo>=redCount)
                {
                    im.setPixelColor(ii,jj,Qt::black);
                    continue;
                }
                if(ty==0)
                {
                    im.setPixelColor(ii,jj,Qt::red);
                }
                else if(ty ==1)
                {
                    im.setPixelColor(ii,jj,Qt::green);
                }
                else if(ty==2)
                {
                    im.setPixelColor(ii,jj,Qt::blue);
                }
                redCo += 1  ;
            }
        }
    }


    void draw()
    {
        QPainter painter(this);
        if(reso == 0)
        {
            painter.drawImage(QRect(drawP1,drawP2,pic.width(),pic.height()),pic)    ;
            return;
        }

        int  pa = maxX;
        if (pic.width()*reso < maxX)
            pa = pic.width()*reso;
        int pb = maxY;
        if (pic.height()*reso < maxY)
            pb = pic.height()*reso;
        auto im = QPixmap(QSize(pa,pb)).toImage();
        for (int i=0;i<pic.width();i++)
        {
            for (int j=0;j<pic.height();j++)
            {
                if( (i+1)*reso>maxX || (j+1)*reso>maxY)
                    continue;
                auto cc = pic.pixel(QPoint(i,j));
                auto r = qRed(cc);
                auto g = qGreen(cc);
                auto b = qBlue(cc);
                drawOne(0,r,i,j,im);
                drawOne(1,g,i,j,im);
                drawOne(2,b,i,j,im);
            }
        }
        painter.drawImage(QRect(drawP1,drawP2,im.width(),im.height()),im);

    }



    void  paintEvent(QPaintEvent*)
    {

        draw();
        if (pause ||sock!=nullptr)
            return;
        xS=xS2;yS=yS2;xL = xL2;yL=yL2;
        sock = new QTcpSocket();
        connect(sock, SIGNAL(connected()), this, SLOT(dealConn()));
        sock->connectToHost("10.23.185.230",8899);
    }

    ~MainWindow(){}

    bool eventFilter(QObject *obj, QEvent *e)
    {
        if (e->type()==QEvent::KeyPress )
        {
            QKeyEvent *keyEvent = static_cast<QKeyEvent*>(e);

            if(keyEvent->key()==65)
                l1->setText(QString::number(l1->text().toInt()-10));

            else if(keyEvent->key()==74)
                l3->setText(QString::number(l3->text().toInt()-10));

            else if(keyEvent->key()==87)
                l2->setText(QString::number(l2->text().toInt()-10));

            else if(keyEvent->key()==73)
                l4->setText(QString::number(l4->text().toInt()-10));

            else if(keyEvent->key()==75)
                l4->setText(QString::number(l4->text().toInt()+10));

            else if(keyEvent->key()==76)
                l3->setText(QString::number(l3->text().toInt()+10));

            else if(keyEvent->key()==83)
                l2->setText(QString::number(l2->text().toInt()+10));

            else if(keyEvent->key()==81)
                l5->setText(QString::number(l5->text().toInt()-3));

            else if(keyEvent->key()==69)
                l5->setText(QString::number(l5->text().toInt()+3));

            else if(keyEvent->key()==68)
                l1->setText(QString::number(l1->text().toInt()+10));

            else
            {
                return QWidget::eventFilter( obj, e );
            }
            onCli();
            e->accept();
            return true;
        }
    }


public slots:
    void onT()
    {
        repaint();
    }

    void onCli()
    {
        xS2 =  l1->text().toInt();
        yS2 =  l2->text().toInt();
        xL2 =  l3->text().toInt();
        yL2 =  l4->text().toInt();
        reso =  l5->text().toInt();
        if (reso<0) reso=0;
        reso = int(reso/3)*3;
    }

    void dealConn()
    {
        connect(sock, SIGNAL(readyRead()), this, SLOT(dealRead()));
        connect(sock, SIGNAL(disconnected()), this, SLOT(dealClose()));
        QByteArray a;
        a.append((char*)&xS,4);
        a.append((char*)&yS,4);
        a.append((char*)&xL,4);
        a.append((char*)&yL,4);
        sock->write(a.data(),a.size());
    }

    void dealClose()
    {
        sock->close();
        sock->deleteLater();
        sock = nullptr;
        if(data.size()==0)return;
        auto im = QPixmap(QSize(xL,yL)).toImage();
        char* temp = data.data();
        int argb ;

        for(int j=0;j<yL;j++)
        {
            for(int i=0;i<xL;i++)
            {
                int val =  i+j*xL;

                memcpy((char*)&argb,temp+val*4,4);
                im.setPixelColor(i,j,QColor( (argb>>0)&0xFF, (argb>>8)&0xFF, (argb>>16)&0xFF));
            }
        }
        data.clear();
        pic = im    ;
        repaint();
    }
    void dealRead()
    {
        data.append(sock->readAll());
    }
};




#endif
