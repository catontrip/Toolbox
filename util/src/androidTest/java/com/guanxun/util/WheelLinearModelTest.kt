package com.guanxun.util

import android.util.Log
import org.junit.jupiter.api.Assertions.*

//import org.junit.jupiter.api.Test
import org.junit.Test
import java.lang.Math.random
import kotlin.math.abs

class WheelLineModelTest {
    private var w = WheelLinearModel()

    @Test
    fun coordOfIndex() {
        coordOfIndex4Empty()
    }

    private fun coordOfIndex4Empty() {
        w.itemCount = 0
        assertEquals(0, w.coordOfIndex(0))
    }

    @Test
    fun coordOfIndexA() {
        w.apertureMaxLength = 150
        w.itemCount = 10
        w.itemSize = 20

        for (i in 0..9) {
            val x = w.coordOfIndex(i)
            val dist = (random() * 1000).toInt()
            Log.d(TAG, "coordOfIndexA: $i,$x")
            assertEquals((w.itemSize * i + w.focusCenter - w.itemSize / 2).toInt() % 200, x % 200)
            w.turn(1)
            val expect = x + 1
            assertEquals(expect, w.coordOfIndex(i))
            w.turn(-1)
            w.turn(dist)
            val delta = dist % (w.itemSize * w.itemCount)
            val newX = w.coordOfIndex(i)
            val flag =
                x + delta == newX || x - delta == newX || (w.itemSize * w.itemCount) - x + newX == delta
            if (!flag) {
                Log.d("TAG", "xByIndexA: i=$i: x=$x newX=$newX delta=$delta")
            }
            assertTrue(flag)
            w.turn(-delta)
        }
    }

    @Test
    fun coordOfIndexB() {
        w.apertureMaxLength = 140
        w.itemCount = 10
        w.itemSize = 20

        for (i in 0..10 * 10) {
            val ind = i % 10
            val dist = (random() * 3000).toInt()
            val x = w.coordOfIndex(ind)
            w.turn(dist)
            w.turn(-dist)
            assertEquals(x, w.coordOfIndex(ind))
            Log.d("$dist", "xByIndexB: $ind $x")
            assertTrue(x <= w.itemSize * (w.itemCount - 1) && x > -w.itemSize)
            w.turn(-dist)
            w.turn(+dist)
            assertEquals(x, w.coordOfIndex(ind))
        }
    }

    @Test
    fun coordOfIndexC() {
        w.apertureMaxLength = 17
        w.itemCount = 5
        w.itemSize = 5
        assertEquals(5, w.coordOfIndex(0))
        w.turn(10)
        assertEquals(15, w.coordOfIndex(0))
        assertEquals(0, w.coordOfIndex(2))
        w.turn(5)
        assertEquals(0, w.coordOfIndex(1))
        ////
        w.apertureMaxLength = 17
        w.itemCount = 2
        w.itemSize = 5
        w.turn(-5)
        assertEquals(0, w.coordOfIndex(0))
        w.turn(-5)
        assertEquals(0, w.coordOfIndex(0))
        w.turn(15)
        assertEquals(5, w.coordOfIndex(0))
    }

    @Test
    fun indexOfWindowCenter() {
        w.apertureMaxLength = 140
        w.itemCount = 10
        w.itemSize = 20
        assertEquals(0, w.indexOfItemAtFocus())
        w.turn(9)
        assertEquals(0, w.indexOfItemAtFocus())
        w.turn(1)
        assertEquals(9, w.indexOfItemAtFocus())
        w.turn(1)
        assertEquals(9, w.indexOfItemAtFocus())
        w.turn(20)
        assertEquals(8, w.indexOfItemAtFocus())
        w.turn(20)
        assertEquals(7, w.indexOfItemAtFocus())
        w.turn(20)
        assertEquals(6, w.indexOfItemAtFocus())
        w.turn(-200)
        assertEquals(6, w.indexOfItemAtFocus())
        w.turn(200)
        assertEquals(6, w.indexOfItemAtFocus())
        w.turn(220)
        assertEquals(5, w.indexOfItemAtFocus())
        w.turn(-20)
        assertEquals(6, w.indexOfItemAtFocus())
    }

    @Test
    fun turnNToCenter() {
        w.apertureMaxLength = 130
        w.itemCount = 10
        w.itemSize = 20
        w.turnNToCenter(0)
        assertEquals((w.focusCenter - w.itemSize / 2).toInt(), w.coordOfIndex(0))
        w.turnNToCenter(6)
        assertEquals((w.focusCenter - w.itemSize / 2).toInt(), w.coordOfIndex(6))
        for (i in 0..1000) {
            val n: Int = (random() * 10).toInt()
            w.turnNToCenter(n)
            Log.d("TAG", "turnNToCenter: $n,${(10 + n - 3) % 10}")
            assertEquals((w.focusCenter - w.itemSize / 2).toInt(), w.coordOfIndex(n))
//      assertEquals(0,w.coordOfIndex((10+n-3)%10))
        }
    }

    @Test
    fun turnNToCenterB() {
        for (i in 0..10000) {
            randomSetting(w)
            val n: Int = (random() * w.itemCount).toInt()
            w.turnNToCenter(n)
            assertEquals(
                (w.focusCenter - w.itemSize / 2).toInt(), w.coordOfIndex(n),
                "turnNToCenterB: $n, itemSize=${w.itemSize}, count=${w.itemCount}, vc=${w.visibleItemCount}"
            )
        }
    }

    private fun randomSetting(w: WheelLinearModel) {
        val itemLen: Int = 50 + (random() * 150).toInt()
        val visibleItems = 1 + (random() * 10).toInt()
        val itemCount: Int = visibleItems + (random() * 10).toInt()
        w.apertureMaxLength = visibleItems * itemLen + (random() * itemLen).toInt()
        w.itemCount = itemCount
        w.itemSize = itemLen
    }

    @Test
    fun distanceOfNToCenter() {
        for (i in 0..10000) {
            randomSetting(w)
            val turn: Int = (random() * w.itemSize).toInt()
            w.turn(turn)
            val ind = w.indexOfItemAtFocus()
            val dis = w.distanceOfNToCenter(ind)
            Log.d(TAG, "distanceOfNToCenter: $turn, $dis,${w.itemSize}, ${w.itemCount}")
            assertTrue(
                abs(dis) <= w.itemSize / 2 + 1,
                "distanceOfNToCenter: turn=$turn, dist=$dis, itemSize=${w.itemSize}, count=${w.itemCount}"
            )
        }
    }

    @Test
    fun distanceOfNToCenterX() {
        w.itemSize = 189
        w.itemCount = 9
        w.apertureMaxLength = w.itemSize * 3
        val turn: Int = 94
        w.turn(turn)
        val ind = w.indexOfItemAtFocus()
        val dis = w.distanceOfNToCenter(ind)
        Log.d(TAG, "distanceOfNToCenter: $turn, $dis,${w.itemSize}, ${w.itemCount}")
        assertTrue(
            abs(dis) <= w.itemSize / 2 + 1,
            "distanceOfNToCenter: turn=$turn, dist=$dis, itemSize=${w.itemSize}, count=${w.itemCount}"
        )
    }

    @Test
    fun distanceOfNToCenterA() {
        w.itemSize = 82
        w.itemCount = 1
        w.apertureMaxLength = w.itemSize * 3
        val turn: Int = 30
        w.turn(turn)
        val ind = w.indexOfItemAtFocus()
        val dis = w.distanceOfNToCenter(ind)
        Log.d(TAG, "distanceOfNToCenter: $turn, $dis,${w.itemSize}, ${w.itemCount}")
        assertTrue(
            abs(dis) <= w.itemSize / 2 + 1,
            "distanceOfNToCenter: turn=$turn, dist=$dis, itemSize=${w.itemSize}, count=${w.itemCount}"
        )
    }

    @Test
    fun pointOfIndex() {
        for (i in 0..10000) {
            randomSetting(w)
            val index = (random() * w.itemCount).toInt()
            val relativePositionWithinItem = (random() * w.itemSize).toInt()
            if(relativePositionWithinItem==0)continue
            val coord = (index * w.itemSize + relativePositionWithinItem+w.offSet)%(w.itemCount*w.itemSize)
            val result = w.pointOfIndex(coord)
            assertNotNull(result)
            result?.let {
                assertEquals(index, it.index,"$i, $index, ${w.itemSize}, $coord")
            }
        }
    }
}