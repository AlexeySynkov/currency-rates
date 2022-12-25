package ru.sber.study.demo.repository

import org.springframework.stereotype.Repository
import ru.sber.study.demo.enum.UserState

@Repository
class UserRepository {
    val map = HashMap<Long, UserState>()

    fun addUser(id: Long) {
        if (map[id] != null) map[id] = UserState.STARTED
    }

    fun getUserState(id: Long): UserState {
        if (map[id] == null) {
            addUser(id)
        }
        return map[id]!!
    }

    fun setUserState(id: Long, newState: UserState) {
        if (map[id] == null) {
            addUser(id)
        }
        map[id] = newState
    }
}