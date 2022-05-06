import os
import sqlite3
import json
import re
from bs4 import BeautifulSoup

#解析html
def parseHtml():
    #dirPath = os.getcwd()
    dirPath = "D:\\爬虫\\新建文件夹\\html"
    dirlist = os.listdir(dirPath)
    dirlist.sort(key = lambda x:int(x[7:-5]))
    contentMaxLen = 700#内容最大字数
    banned = open("banned.txt",encoding="utf-8")
    banList = banned.read()
    banned.close()
    conn=sqlite3.connect("companys.db")
    for f in dirlist:
        file = open(dirPath+"\\"+f,mode="r",encoding="utf-8")
        htmlStr = file.read()
        file.close()
        soup = BeautifulSoup(htmlStr,"lxml")
        descTag = soup.find("meta",attrs={"name":"description"})

        company_name = soup.find("strong").text
        company_desc = ""

        if("content" in descTag.attrs):
            company_desc = descTag["content"]
        else:
            continue
        company_name = getSafeString(company_name)
        company_desc = getSafeString(company_desc)

        root = []
        for i in soup.find_all("div",attrs={"class":"commentType"}):
            mainComment = i.find("div",attrs={"class":"comment_txt pull-left"})
            if(len(mainComment.p.text)<contentMaxLen):#超过contentMaxLen字，判定为垃圾评论
                content = mainComment.p.text
                if(content in banList):#包含铭感词的，跳过
                    continue
                content = getSafeString(content)
                date = mainComment.find("div",attrs={"class":"pull-left"}).text
                #print(content+"----->"+date)#主评论
                subComments = i.find_all("div",attrs={"class":"comment_child"})
                childs = []
                for s in subComments:
                    if(len(s.p.text)<contentMaxLen):#超过contentMaxLen字，判定为垃圾评论
                        subContent = s.p.text
                        if(content in banList):#包含铭感词的，跳过
                            continue
                        subContent = getSafeString(subContent)
                        subDate = s.find("div",attrs={"class","pull-left"}).text
                        #print("    "+subDate)#子评论
                        childs.append({"subContent":subContent,"subDate":subDate})
                root.append({"content":content,"date":date,"childs":childs,"extra":""})
        company_comments = json.dumps(root,ensure_ascii=False)

        #这里执行插入语句
        exe = "INSERT INTO company(company_name,company_description,company_comment) \
        VALUES('"+company_name+"','"+company_desc+"','"+company_comments+"')"
        conn.execute(exe)
        conn.commit()
        print(f+"插入完成")
    conn.close()

#获取安全的字符串，防止sql注入和插入报错
def getSafeString(str):
    str = str.replace("'", "")
    str = str.replace("\"", "")
    str = str.replace("&", "&amp")
    str = str.replace("<", "&lt")
    str = str.replace(">", "&gt")
    str = str.replace("delete", "")
    str = str.replace("update", "")
    str = str.replace("insert", "")
    return str

#创建表
def createDB():
    dbFileName = "companys.db"
    if(os.path.exists(dbFileName)):
        return
    conn=sqlite3.connect(dbFileName)
    print("数据库创建成功")
    conn.execute("""
        CREATE TABLE company(
        company_id INTEGER PRIMARY KEY AUTOINCREMENT,
        company_name TEXT NOT NULL,
        company_description TEXT NOT NULL,
        company_comment TEXT NOT NULL,
        str1 TEXT,
        str2 TEXT)
        """)
    conn.close()
    print("company表创建成功")

createDB()
parseHtml()
