# Route Segmentation
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/route_segmentation.png)
Instead of calculating a point-to-point route by ourselves, we decide to make use of the Google Directions API for a better performance (We could barely beat Google).
Given two end points A and B, we first query the Google Directions API to fetch a direct route from A to B.
Then we cut the entire route into segments according to the mile range (let's say 160 miles) of the user's vehicle and search for the "nearest" charging station at the end of each segment.
Now we get a new path from A->C->D->B and we simply send these points to the Directions API again and plot the final route.
# Searching Methods
## KD-Tree
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/KDtree.png)
A KD-Tree (short for k-dimensional tree) is a space-partitioning data structure for organizing points in a k-dimensional space. 
The graph above briefly illustrates a situation where k=2. We sort the data points with respect to their x-coordinates and y-coordinates alternately following the binary search rule.
Consult [this video](https://www.bing.com/videos/search?q=kd+tree+geeksforgeeks&qpvt=kd+tree+geeksforgeeks&view=detail&mid=F0618F0AF8A08E6D7392F0618F0AF8A08E6D7392&&FORM=VRDGAR&ru=%2Fvideos%2Fsearch%3Fq%3Dkd%2Btree%2Bgeeksforgeeks%26qpvt%3Dkd%2Btree%2Bgeeksforgeeks%26FORM%3DVDRE) for more details.
In the our case of charging stations, we use the latitudes and longitudes of each charger to store the data and use binary search to search the tree for the first "near" neighbor within a certain tolerant distance (e.x.5 miles)
This provides a suboptimal solution that guarantees a timeplexity of O(log(n)). \
Performance comparison with another app: \
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/performance%20comparison.gif)
This approach gives quick searching speed but sacrifies a lot of local memory since we need to store the entire data structure on the mobile local memory. That is why we can only serve one state(CA) with this method.
## API query
In our latest commit, a query to the [NREL](https://developer.nrel.gov/) is implemented, which directly gives the nearest charging station to a given point. We also added a filter feature which searches the desired charging station only. This allows us to provide a wider range of service that covers the entire United States. \
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/path_ca_ny.png)
