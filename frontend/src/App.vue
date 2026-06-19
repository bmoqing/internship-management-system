<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 唯一的任务就是显示当前路由对应的页面 -->
  <RouterView />
</template>

<style>
/* 这一步很关键！重置默认的边距，否则你的侧边栏会有白边 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
}
</style>