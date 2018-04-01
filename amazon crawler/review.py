# -*- coding: utf-8 -*-
"""

"""

import urllib
from bs4 import BeautifulSoup
import time
import random
import pymysql.cursors


def getCount(name,url):
    #if same product already exists return -1
    connection  = pymysql.connect(host = 'localhost',
                                  user = 'root',
                                  password = '123456',
                                  db = 'amazon',
                                  charset = 'utf8mb4')
    try:
        with connection.cursor() as cursor:
            sql = "select * from products where name=(%s)"

            cursor.execute(sql,(name))
            results = cursor.fetchall()
            if len(results)>0:
                return -1

    finally:
        connection.close()
    #get basic information about the product
    html1 = urllib.urlopen(url).read()
    html1 = str(html1)
    soup1 = BeautifulSoup(html1,'lxml')
    productCount = soup1.find_all(attrs={"data-hook":"total-review-count"})
    productAverageRating = soup1.find_all(attrs={"data-hook":"average-star-rating"})
    productPrice = soup1.find_all('span',{'class': 'a-color-price arp-price'})

    if len(productCount[0].string)>3:
        numbers = productCount[0].string.split(',')
        count = int(numbers[0])*1000+int(numbers[1])
    else:
        count = int(productCount[0].string)
    averageRating = productAverageRating[0].string
    price = productPrice[0].string

    connection  = pymysql.connect(host = 'localhost',
                                  user = 'root',
                                  password = '123456',
                                  db = 'amazon',
                                  charset = 'utf8mb4')
    try:
        with connection.cursor() as cursor:
            sql = "insert into `products` (name,price,totalReviewCount,averageRating) values (%s,%s,%s,%s)"

            cursor.execute(sql,(name,price,count,averageRating))


            connection.commit()
    finally:
        connection.close()
    
    return count
    
def crawler(name,url):
    html1 = urllib.urlopen(url).read()
    html1 = str(html1)

    soup1 = BeautifulSoup(html1,'lxml')
    reviewAuthor = soup1.find_all(attrs={"data-hook":"review-author"})
    reviewDate = soup1.find_all(attrs={"data-hook":"review-date"})
    reviewText = soup1.find_all(attrs={"data-hook":"review-body"})
    reviewRating = soup1.find_all(attrs={"data-hook":"review-star-rating"})
    reviewTitle = soup1.find_all(attrs={"data-hook":"review-title"})
    

    index = 0
    for cTime in reviewDate:
        commentTime = cTime.string
        text = reviewText[index].string
        rating = reviewRating[index].string
        title = reviewTitle[index].string
        author = reviewAuthor[index].string
        index=index+1


        connection  = pymysql.connect(host = 'localhost',
                                  user = 'root',
                                  password = '123456',
                                  db = 'amazon',
                                  charset = 'utf8mb4')
        try:
            with connection.cursor() as cursor:
                sql = "insert into `reviews` (comment,date,rating,title,author,productName) values (%s,%s,%s,%s,%s,%s)"

                cursor.execute(sql,(text,commentTime,rating,title,author,name))


                connection.commit()
        except:
            continue
        finally:
            connection.close()


links = []
file = open("parseddata.txt") 
while 1:
    line = file.readline()
    if not line:
        break
    else:
        links.append(line)
    pass # do something
file.close()
for i in range(1,len(links)):
    
    temp = links[i-1].split('/')
    print(i)
    tail = temp[7].split('&')
    productName = temp[3]
    print("product: "+productName)
    url = "https://www.amazon.com/"+productName+"/product-reviews/"+temp[5]+"/"+temp[6]+"/"+tail[0]+"&ie=UTF8&reviewerType=avp_only_reviews&sortBy=recent"
    count = getCount(productName,url)
    if count<0:
        print("same product exists")
    else:
        page = count/10 + 2
        for j in range(1,page):
            print("downloading reviews from page{}...".format(j))
            productUrl =url + "&pageNumber="+str(j)
            crawler(productName,productUrl)
