from bs4 import BeautifulSoup
import requests

file = open(r"parseddata.txt", "w")
for i in range(1,401):
    print("getting links from page{}...".format(i))
    url = "https://www.amazon.com/s/ref=sr_pg_"+str(i)+"?sf=qz%2Crba&fst=as%3Aoff&rh=n%3A3375251%2Ck%3Anike%2Cp_89%3ANIKE&page="+str(i)+"&keywords=nike&unfiltered=1&ie=UTF8"

    # add header
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36'
    }
    r = requests.get(url, headers=headers)
    soup = BeautifulSoup(r.content, "lxml")

    links = soup.find_all('a', {'class': 'a-size-small a-link-normal a-text-normal'})

    for link in links:
		if link.get('href').find("customerReviews")>0:
			file.write(link.get('href')+ '\n')
		
file.close()