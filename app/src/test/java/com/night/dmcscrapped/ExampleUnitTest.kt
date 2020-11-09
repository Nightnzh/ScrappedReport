package com.night.dmcscrapped

import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun test() {
        val model = "5*2"
        val pcs = "2,4,6,8,10,1,3,5,7,9"
//        Log.d("@@@pcsTest",changePcs("5*2","2,4,6,8,10,1,3,5,7,9",0,90) )
        print(changePcs("5*2","2,4,6,8,10,1,3,5,7,9",0,270))

    }

    fun changePcs(model:String,pcs:String,display:Int,displayDegree:Int): String {

        var wh = model.split("*").map { it.toInt() }
//        println("@@@$wh")
        val pcsAry = pcs.split(",").map { it.toInt() }

        var map = ArrayList<ArrayList<Int>>()
//        map.add(intArrayOf(4,3))

        for( i in 0 until wh[1]){
            map.add(arrayListOf())
            for(j in 0 until wh[0]){
                map[i].add(pcsAry[i*wh[0]+j])
            }
        }

        when(displayDegree){
            90 ->{
                wh = wh.reversed()
                val newMap = ArrayList<ArrayList<Int>>()
                for(i in 0 until wh[1]){
                    newMap.add(arrayListOf())
                    var k = wh[0]-1
                    for(j in 0..k){
                       newMap[i].add(map[k][i])
                        k--
                    }
                }
                map = newMap
            }
            180 -> {
                map.apply {
                    forEach {
                        it.reverse()
                    }
                    reverse()
                }
            }
            270 -> {
                wh = wh.reversed()
                val newMap = ArrayList<ArrayList<Int>>()
                var k = wh[1]-1

                for(i in 0..k){
                    newMap.add(arrayListOf())
                    for (j in 0 until wh[0]){
                        newMap[i].add(map[j][k])
                    }
                    k--
                }
                map = newMap
            }
        }

        if(display == 2){
            map.apply {
                map{
                    it.reverse()
                }
            }
        }

        val out = map.map { it.joinToString { it.toString() } }.joinToString { "$it" }

        return  out
    }
}