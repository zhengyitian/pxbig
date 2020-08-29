#ifndef MAINWINDOW_H
#define MAINWINDOW_H
#include "util.h"
#include "qmap.h"


class MainWindow : public QWidget
{
    Q_OBJECT
public:

    QImage lastDrawIm;

    QMap<QString,int> coMap;
    int lineLen = 1;
    int boxLen = 20;
    int coX = 10;
    int coY = 30;
    QLabel * la = new QLabel(this);

    Qt::GlobalColor oriColor = Qt::black,lineColor=Qt::gray,drawColor=Qt::white;
    explicit MainWindow(QWidget *parent = nullptr)
    {
        lastDrawIm = QPixmap(QSize(coX*boxLen,coY*boxLen)).toImage();
        lastDrawIm.fill(oriColor);
        drawLine();
        QCoreApplication::instance()->installEventFilter(this);
        la->setGeometry(coX*boxLen+50,0,100,100);
        la->setText(QString::number(coMap.size()));
    }

    void drawLine()
    {
        for (int i=1;i<coX;i++)
        {
            for(int j=i*boxLen-lineLen;j<=i*boxLen+lineLen;j++)
            {
                for(int k=0;k<coY*boxLen;k++)
                    lastDrawIm.setPixelColor(j,k,lineColor);
            }
        }

        for (int i=1;i<coY;i++)
        {
            for(int j=i*boxLen-lineLen;j<=i*boxLen+lineLen;j++)
            {
                for(int k=0;k<coX*boxLen;k++)
                    lastDrawIm.setPixelColor(k,j,lineColor);
            }
        }
    }



    void  paintEvent(QPaintEvent* e)
    {
        QWidget::paintEvent(e);
        QPainter painter(this);
        painter.drawImage(QRect(0,0,lastDrawIm.width(),lastDrawIm.height()),lastDrawIm);
    }

    void dealFind(int i,int j)
    {
        auto k = QString::number(i)+" "+QString::number(j);
        if(coMap.contains(k))
        {
            coMap.remove(k);
            for(int ii=(i-1)*boxLen;ii<i*boxLen;ii++)
            {
                for(int jj=(j-1)*boxLen;jj<j*boxLen;jj++)
                {
                    lastDrawIm.setPixelColor(ii,jj,oriColor);
                }
            }
        }

        else {
            coMap[k]=1;
            for(int ii=(i-1)*boxLen;ii<i*boxLen;ii++)
            {
                for(int jj=(j-1)*boxLen;jj<j*boxLen;jj++)
                {
                    lastDrawIm.setPixelColor(ii,jj,drawColor);
                }
            }
        }
    }

    bool eventFilter(QObject *obj, QEvent *e)
    {
        if(e->type()!=QEvent::MouseButtonPress )
            return false;
        QMouseEvent* me = static_cast<QMouseEvent*>(e);
        auto x =  me->x();auto y = me->y();
        if(x>=coX*boxLen||y>=coY*boxLen)
            return false;

        for(int i=1;i<=coX;i++)
        {
            for(int j=1;j<=coY;j++)
            {
                if(x<=boxLen*i&&y<=j*boxLen)
                {
                    dealFind(i,j);
                      la->setText(QString::number(coMap.size()));
                    drawLine();
                    qDebug()<<i<<j<<coMap.size();
                    update();
                    return true;
                }
            }
        }
    }

};

#endif
