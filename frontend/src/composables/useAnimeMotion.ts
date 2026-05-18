import { nextTick, onBeforeUnmount, type Ref } from 'vue'
import { animate, stagger, type AnimationParams, type JSAnimation } from 'animejs'

type ElementRoot = Ref<HTMLElement | undefined>

const reducedMotionQuery = '(prefers-reduced-motion: reduce)'

function shouldReduceMotion() {
  return typeof window !== 'undefined' && window.matchMedia(reducedMotionQuery).matches
}

function query(root: HTMLElement | undefined, selector: string) {
  if (!root) return []
  return Array.from(root.querySelectorAll<HTMLElement>(selector))
}

export function useAnimeMotion() {
  const activeAnimations: JSAnimation[] = []

  function run(targets: Parameters<typeof animate>[0], params: AnimationParams) {
    if (shouldReduceMotion()) return undefined
    const animation = animate(targets, params)
    activeAnimations.push(animation)
    return animation
  }

  async function enterPage(root: ElementRoot) {
    await nextTick()
    const items = query(root.value, '[data-motion="page-item"]')
    if (!items.length) return
    run(items, {
      opacity: { from: 0 },
      y: { from: 14 },
      duration: 460,
      delay: stagger(58),
      ease: 'outCubic'
    })
  }

  async function enterDashboard(root: ElementRoot) {
    await nextTick()
    const metrics = query(root.value, '.metric-card')
    const panels = query(root.value, '.panel')
    if (metrics.length) {
      run(metrics, {
        opacity: { from: 0 },
        y: { from: 18 },
        scale: { from: 0.98 },
        duration: 520,
        delay: stagger(72),
        ease: 'outCubic'
      })
    }
    if (panels.length) {
      run(panels, {
        opacity: { from: 0 },
        y: { from: 16 },
        duration: 560,
        delay: stagger(82, { start: 120 }),
        ease: 'outCubic'
      })
    }
  }

  function pulse(target: HTMLElement | undefined) {
    if (!target) return
    run(target, {
      scale: [{ to: 0.96, duration: 90 }, { to: 1, duration: 260 }],
      ease: 'outCubic'
    })
  }

  async function revealLatest(root: ElementRoot, selector: string) {
    await nextTick()
    const items = query(root.value, selector)
    const latest = items[items.length - 1]
    if (!latest) return
    run(latest, {
      opacity: { from: 0 },
      y: { from: 10 },
      scale: { from: 0.985 },
      duration: 300,
      ease: 'outCubic'
    })
  }

  async function openFloating(root: ElementRoot, selector: string) {
    await nextTick()
    const target = query(root.value, selector)[0]
    if (!target) return
    run(target, {
      opacity: { from: 0 },
      y: { from: 8 },
      scale: { from: 0.98 },
      duration: 220,
      ease: 'outCubic'
    })
  }

  onBeforeUnmount(() => {
    activeAnimations.splice(0).forEach((animation) => animation.cancel())
  })

  return {
    enterPage,
    enterDashboard,
    pulse,
    revealLatest,
    openFloating
  }
}
