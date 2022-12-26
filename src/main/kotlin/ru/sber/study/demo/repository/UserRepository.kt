package ru.sber.study.demo.repository

import org.springframework.stereotype.Repository
import ru.sber.study.demo.enum.UserState

@Repository
class UserRepository {
    val map = HashMap<Long, UserData>()

    fun addUser(id: Long): UserData {
        map[id] = UserData(UserState.STARTED)
        return map[id]!!
    }

    fun getUserState(id: Long): UserState {
        if (!map.containsKey(id)) {
            addUser(id)
        }
        return map[id]!!.state
    }

    fun setUserState(id: Long, newState: UserState) {
        if (!map.containsKey(id)) {
            addUser(id)
        }
        map[id]!!.state = newState
    }

    fun getUserData(id: Long): UserData = if (!map.containsKey(id)) {
        addUser(id)
    } else {
        map[id]!!
    }
}