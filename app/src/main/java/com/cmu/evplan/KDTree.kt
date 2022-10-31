package com.cmu.evplan

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.nio.file.spi.FileTypeDetector

class Node (var marker: MarkerType,
            var left: Node? = null,
            var right: Node? = null)

class KDTree () {
    var root: Node? = null

    private fun insertRec(node: Node, marker: MarkerType, depth: Int) {
        var latlng = marker.location
        var curdim = depth % 2
        if (curdim == 0 && latlng.latitude < node.marker.location.latitude
            || curdim == 1 && latlng.longitude < node.marker.location.longitude){
            if (node.left != null) {
                insertRec(node.left!!, marker, depth + 1)
            }
            else {
                node.left = Node(marker)
            }
        }
        else {
            if (node.right != null) {
                insertRec(node.right!!, marker, depth + 1)
            }
            else {
                node.right = Node(marker)
            }
        }

    }
    fun insert(marker: MarkerType) {
        if (root == null){
            var node = Node(marker)
            root = node
        }
        else {
            insertRec(root!!, marker, 0)
        }
    }

    fun search(latlng: LatLng, distance: Float) : Node? {
        return searchRec(root!!, latlng, distance, 0)
    }

    private fun searchRec(node: Node, latlng: LatLng, distance: Float, depth: Int) : Node {
        var temp = FloatArray(1)
        Location.distanceBetween(latlng.latitude, latlng.longitude, node.marker.location.latitude, node.marker.location.longitude, temp)
        if (temp[0] <= distance) {
            return node
        }
        var curdim = depth % 2
        if (curdim == 0 && latlng.latitude < node.marker.location.latitude
            || curdim == 1 && latlng.longitude < node.marker.location.longitude){
            if (node.left == null) {
                return node
            }
            return searchRec(node.left!!, latlng, distance, depth + 1)
        }
        else {
            if (node.right == null) {
                return node
            }
            else {
                return searchRec(node.right!!, latlng, distance, depth + 1)
            }
        }

    }

}