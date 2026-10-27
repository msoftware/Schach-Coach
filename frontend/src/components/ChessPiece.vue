<script setup lang="ts">
import { computed } from 'vue';
import { pieceLabel, type PieceCode } from '@/utils/fen';
import wK from '@/assets/chess-pieces/wK.svg?url';
import wQ from '@/assets/chess-pieces/wQ.svg?url';
import wR from '@/assets/chess-pieces/wR.svg?url';
import wB from '@/assets/chess-pieces/wB.svg?url';
import wN from '@/assets/chess-pieces/wN.svg?url';
import wP from '@/assets/chess-pieces/wP.svg?url';
import bK from '@/assets/chess-pieces/bK.svg?url';
import bQ from '@/assets/chess-pieces/bQ.svg?url';
import bR from '@/assets/chess-pieces/bR.svg?url';
import bB from '@/assets/chess-pieces/bB.svg?url';
import bN from '@/assets/chess-pieces/bN.svg?url';
import bP from '@/assets/chess-pieces/bP.svg?url';

const SOURCES: Record<PieceCode, string> = { wK, wQ, wR, wB, wN, wP, bK, bQ, bR, bB, bN, bP };

const props = defineProps<{ piece: PieceCode; decorative?: boolean }>();
const src = computed(() => SOURCES[props.piece]);
const label = computed(() => pieceLabel(props.piece));
</script>

<template>
  <img
    class="chess-piece"
    :src="src"
    :alt="decorative ? '' : label"
    :title="decorative ? undefined : label"
    draggable="false"
    :data-piece="piece"
  />
</template>

<style scoped>
.chess-piece {
  width: 100%;
  height: 100%;
  display: block;
  user-select: none;
  pointer-events: none;
  -webkit-user-drag: none;
}
</style>
