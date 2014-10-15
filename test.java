 /**
     *  s[i] = s[i-1] + s[i-2]
     *    
     *   maybe a very big number
     *   
     * @see [类、类#方法、类#成员]
     */
    public long calc()
    {
        long[] arrays = new long[100];
        arrays[0] = 1;
        arrays[1] = 1;
        
        for (int i = 2; i < arrays.length; i++)
        {
            arrays[i] = arrays[i - 1] + arrays[i - 2];
        }
        
       return arrays[99];
    }

     
    
