package utils;

import Sensor.Simulation_Task;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * SensorCmdHandler 用于处理模拟器接收到的指令，进行相应的操作；若该命令无法识别，则 返回拒绝信息
 * 
 * @author Yibing Zhang
 */
public class SensorCmdHandler
{
    private String          cmd;       // 模拟器接收到的命令
    private Simulation_Task task;      // 模拟器数据发送模块
    private String          sensorName;// 模拟器名称
    private SerialPort      port;      // 串口号
    private String          fileName;  // 文件名称
    private int             fileType;  // 文件种类，2进制或非2进制
    private long            interval;  // 设置间隔


    public SensorCmdHandler(String cmd)
    {
        this.cmd = cmd;
    }


    /**
     * 设置handler的参数
     * 
     * @param task
     *            模拟器数据发送模块
     * @param port
     *            串口
     * @param fileName
     *            文件名称
     * @param fileType
     *            文件种类
     */
    public void iniHandler(Simulation_Task task, SerialPort port, String fileName, int fileType, long interval)
    {
        this.task = task;
        this.sensorName = task.getsensorName();
        this.port = port;
        this.fileName = fileName;
        this.fileType = fileType;
        this.interval = interval;
    }


    /**
     * 处理相应的指令，首先判断是否命令跟随参数，若有，再进行识别该命令和参数的识别；若无，直接识别该命令并
     * 做出相应的工作。该方法会返回一个模拟器数据发送模块，方便主程序对当前模块的更新
     * 
     * @return 模拟器数据发送模块
     */
    public Simulation_Task doWork()
    {
        // 命令带有参数的
        if (cmd.contains(" "))
        {
            String[] cmds = cmd.split(" ");
            switch (cmds[0])
            {
                case "br":
                    task = changeRate(cmds[1]);
                    break;
                case "setInterval":
                    setInterval(cmds[1]);
                    break;
                default:
                    SerialPortManager.sendToPort(port, ("Can't recognize this command\n").getBytes());
            }
        }
        // 命令不带有参数的
        else
        {
            switch (cmd)
            {
                case "off":
                    turnOff();// 调用关闭函数
                    break;
                case "on":
                    task = turnOn();// 调用开始函数
                    break;
                default:
                    SerialPortManager.sendToPort(port, ("Can't recognize this command\n").getBytes());
            }
        }
        return task;
    }


    /**
     * 关闭传感器（关闭模拟器数据发送模块）
     */
    private void turnOff()
    {
        // 判断仪器是否已关闭
        if (task.getRunningState())
        {
            task.stop();
            SerialPortManager.sendToPort(port, (sensorName + " has Stopped\n").getBytes());
        }
        else
        {
            SerialPortManager.sendToPort(port, (sensorName + " has Stopped\n").getBytes());
        }
    }


    /**
     * 设置采样间隔
     * 
     * @param interval采样间隔
     */
    private void setInterval(String interval)
    {
        this.interval = 100 * Integer.parseInt(interval);
        task.setInterval(this.interval);
    }

    /**
     * 获取采样间隔
     * @return采样间隔
     */
    public long getInterval()
    {
        return interval;
    }


    /**
     * 打开模拟器（打开模拟器数据发送模块） 该方法会返回一个模拟器数据发送模块，方便主程序对当前模块的更新
     * 
     * @return 模拟器数据发送模块
     */
    private Simulation_Task turnOn()
    {
        if (!task.getRunningState())
        {
            task = new Simulation_Task(sensorName, port, fileName, fileType, interval);
            task.setRunningState(true);
            Thread thread = new Thread(task);
            thread.start();
        }
        else
        {
            SerialPortManager.sendToPort(port, (sensorName + " is running\n").getBytes());
        }
        return task;
    }


    /**
     * 更改模特率。先停止数据传输模块，对该模拟器波特率进行更改，更改后再重启数据发送模块
     * 
     * @param rate
     *            波特率
     * @return
     */
    private Simulation_Task changeRate(String rate)
    {
        try
        {
            if (!task.getRunningState())
            {
                SerialPortManager.sendToPort(port, ("\n").getBytes());

                System.out.println("BaudRate before: " + port.getBaudRate());
                SerialPortManager.changeBuadeRate(Integer.parseInt(rate), port);
                System.out.println("BaudRate after: " + port.getBaudRate());
            }
            else
            {
                task.stop();
                SerialPortManager.sendToPort(port, ("\n").getBytes());

                System.out.println("BaudRate before: " + port.getBaudRate());
                SerialPortManager.changeBuadeRate(Integer.parseInt(rate), port);
                System.out.println("BaudRate after: " + port.getBaudRate());

                Thread.sleep(1000);
                task = new Simulation_Task(sensorName, port, fileName, fileType, interval);
                task.setRunningState(true);
                Thread thread = new Thread(task);
                thread.start();
            }
        }
        catch (NumberFormatException e)
        {
            System.err.println("Invalid baudrate");
        }
        catch (UnsupportedCommOperationException e)
        {

            System.err.println("change baudrate error");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        return task;
    }
}
