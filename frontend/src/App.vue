<template>
  <router-view v-slot="{ Component, route }">
    <transition name="route-fade" mode="out-in">
      <component :is="Component" :key="route.matched[0]?.path || route.path" />
    </transition>
  </router-view>
</template>
