#!/usr/bin/env python
# coding: utf-8

# In[ ]:


#!/usr/bin/env python
# coding: utf-8

import matplotlib.pyplot as plt
import numpy as np

def plotgrp(df,title):
    start,end = 1,1
    for i in range(1,len(df)):
      
    
        if df['Time'][i] == 'Started:':
            end = i-1
            subset = df[start:end][1:].astype(float)
            plt.plot(subset.iloc[:,0],subset[:,1],color='r')
            plt.plot(subset.iloc[:,0],subset[:,1],color='g')
            plt.plot(subset.iloc[:,0],subset[:,1],color='b')
            start = i+1 
        
        plt.legend(['X','Y','Z'])
        plt.title(title)





def resizedataseq(df,seqlen):
    totlen = len(df)
    Qx = list(df.iloc[-1:-6:-1,0])
    Qy = list(df.iloc[-1:-6:-1,1])
    Qz = list(df.iloc[-1:-6:-1,2])
    
    
    while totlen != seqlen:
        mx = np.mean(Qx)
        my = np.mean(Qy)
        mz = np.mean(Qz)

        if totlen <seqlen:
            Qx.pop(0)
            Qy.pop(0)
            Qz.pop(0)
            Qx.append(mx)
            Qy.append(my)
            Qz.append(mz)
            #행추가
            df.loc[totlen]=[mx,my,mz]
            totlen = totlen+1
        
        
        elif totlen > seqlen:
            #행삭제
            df = df.iloc[:seqlen,:]
            totlen = seqlen
            break
            
        
    return df

