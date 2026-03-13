package com.example.proyectoajedrez.model
data class LichessPuzzleResponse(
    val game: LichessGame,
    val puzzle: LichessPuzzleInfo
)

data class LichessGame(
    val id: String,
    val pgn: String, // Esto seria toda la partida completa del puzzle
    val players: List<LichessPlayer>
)

data class LichessPlayer(
    val color: String,
    val name: String,
    val rating: Int
)

data class LichessPuzzleInfo(
    val id: String,
    val rating: Int,
    val solution: List<String> // La lista de movimientos ganadores
)

// GET /api/user/{username}
data class LichessUserResponse(
    val id: String,
    val username: String,
    val perfs: UserPerfs?, // Puede ser nulo si el usuario es nuevo
    val online: Boolean = false,
    val title: String? = null // GM, IM, FM, etc.
)

data class UserPerfs(
    val blitz: PerfStats?,
    val rapid: PerfStats?,
    val puzzle: PerfStats?
)

data class PerfStats(
    val rating: Int,
    val games: Int,
    val prog: Int // Progreso reciente
)