package com.example.marthianclean.navigation

import androidx.compose.runtime.Composable

/**
 * 프로젝트 어딘가(MainActivity 등)에서 AppNavGraph()를 호출하고 있을 수 있어서
 * 기존 진입점을 유지하면서 실제 내비게이션은 MarthianNavHost로 위임합니다.
 *
 * 이렇게 하면 AppNavGraph 내부의 파라미터 꼬임/Unresolved reference 문제를 원천 차단합니다.
 */
@Composable
fun AppNavGraph() {
    MarthianNavHost()
}
