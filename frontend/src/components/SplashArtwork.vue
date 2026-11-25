<template>
  <svg class="splash-art" viewBox="0 0 1000 590" aria-hidden="true" focusable="false">
    <defs>
      <radialGradient id="splash-crypt" cx=".50" cy=".63" r=".62">
        <stop stop-color="#6e4831" />
        <stop offset=".57" stop-color="#312219" />
        <stop offset="1" stop-color="#1a1410" stop-opacity="0" />
      </radialGradient>
      <radialGradient id="splash-firelight">
        <stop stop-color="#edac54" stop-opacity=".56" />
        <stop offset="1" stop-color="#b76f35" stop-opacity="0" />
      </radialGradient>
      <radialGradient id="splash-radiance">
        <stop stop-color="#fff4cd" stop-opacity=".94" />
        <stop offset=".25" stop-color="#f8c16a" stop-opacity=".76" />
        <stop offset=".62" stop-color="#df983f" stop-opacity=".34" />
        <stop offset="1" stop-color="#c87e32" stop-opacity="0" />
      </radialGradient>
      <linearGradient id="splash-oak" x2="0" y2="1">
        <stop stop-color="#935a38" />
        <stop offset=".52" stop-color="#663b28" />
        <stop offset="1" stop-color="#37231c" />
      </linearGradient>
      <linearGradient id="splash-brass" x2=".25" y2="1">
        <stop stop-color="#e0b977" />
        <stop offset=".45" stop-color="#b88046" />
        <stop offset="1" stop-color="#704829" />
      </linearGradient>
      <linearGradient id="splash-skin" x2=".8" y2=".95">
        <stop stop-color="#edbd8e" />
        <stop offset="1" stop-color="#a26148" />
      </linearGradient>
      <linearGradient id="splash-steel" x2=".74" y2="1">
        <stop stop-color="#b1a18c" />
        <stop offset=".49" stop-color="#6c6260" />
        <stop offset="1" stop-color="#312a2a" />
      </linearGradient>
      <linearGradient id="splash-cloth" x2=".7" y2="1">
        <stop stop-color="#8e5850" />
        <stop offset="1" stop-color="#39272a" />
      </linearGradient>
      <linearGradient id="splash-linen" x2=".8" y2="1">
        <stop stop-color="#e2c9a3" />
        <stop offset="1" stop-color="#876c53" />
      </linearGradient>
      <linearGradient id="splash-floor" x2="0" y2="1">
        <stop stop-color="#302117" />
        <stop offset="1" stop-color="#221912" stop-opacity=".05" />
      </linearGradient>
      <g id="splash-torch">
        <ellipse cy="-23" rx="51" ry="73" fill="url(#splash-firelight)" />
        <path d="M-10 0H10L5 14H-5ZM-3 13H3L2 44H-2Z" fill="#856044" />
        <path d="M0-45C11-33 15-18 8-7Q0 2-8-6C-19-18-2-28 0-45Z" fill="#cb813d" />
        <path d="M0-29C7-20 6-11 0-7Q-8-10 0-29Z" fill="#f3bc73" />
      </g>
    </defs>

    <g class="art-scene">
      <!-- Lantern-lit stone and flagstones -->
      <ellipse cx="500" cy="304" rx="501" ry="299" fill="url(#splash-crypt)" />
      <path d="M113 458V256C113 156 179 103 287 97V458M712 458V97C821 103 887 156 887 256V458"
            fill="#211812" stroke="#664932" stroke-width="5" opacity=".67" />
      <path d="M281 468V264C281 152 343 91 500 44C657 91 719 152 719 264V468"
            fill="#2c1d17" stroke="#6a4933" stroke-width="15" />
      <path d="M301 444V267C301 164 358 108 500 64C642 108 699 164 699 267V444"
            fill="none" stroke="#a47850" stroke-opacity=".27" stroke-width="3" />
      <path d="M327 440V271C327 185 372 135 500 94C628 135 673 185 673 271V440"
            fill="none" stroke="#9c6e49" stroke-opacity=".24" stroke-width="3" />
      <path d="M280 286H326M280 326H326M280 369H326M280 413H326M674 286H720M674 326H720M674 369H720M674 413H720M336 169L312 152M389 117L371 92M447 86L435 64M552 86L565 64M611 117L629 92M664 169L688 152"
            fill="none" stroke="#34241b" stroke-width="3" opacity=".75" />
      <path d="M119 293H275M119 375H274M724 293H881M724 375H881M175 458V375M222 375V293M781 458V375M824 375V293"
            fill="none" stroke="#8f6448" stroke-opacity=".18" stroke-width="2" />
      <path d="M122 445H878L1000 590H0Z" fill="url(#splash-floor)" />
      <path d="M97 471H903M59 514H941M15 561H985M210 448L158 589M353 448L335 589M500 448V589M647 448L665 589M790 448L842 589"
            fill="none" stroke="#99704e" stroke-opacity=".17" stroke-width="2" />
      <use href="#splash-torch" x="169" y="225" />
      <use href="#splash-torch" x="831" y="225" />
      <ellipse cx="500" cy="515" rx="366" ry="46" fill="#180f0b" opacity=".65" />

      <!-- Barbarian: hide mantle, visible braid, and axe -->
      <g data-class="野蛮人" transform="translate(368 303)">
        <path d="M-46 128L-58 202H-14L-3 148L12 201H52L44 129Z" fill="#352721" />
        <path d="M-68 25L-78 104L-57 112L-43 82M61 24L71 85L52 107L41 83"
              fill="url(#splash-skin)" stroke="#432a23" stroke-width="3" />
        <path d="M-56 17Q0-8 55 16L51 92L35 137H-37L-53 90Z" fill="#8c5239" />
        <path d="M-62 15L-43-2L-27 11L-12 9L-2 22L13 7L33 9L49-4L65 15L63 40L49 35L44 53L26 40L10 52L-3 42L-14 55L-30 38L-46 50L-49 35L-65 36Z"
              fill="#b38b61" stroke="#644635" stroke-width="3" />
        <path d="M-43 26L29 118L41 111L-33 21Z" fill="#59402f" />
        <path d="M-42 91Q-6 99 47 91L43 106Q0 115-41 106Z" fill="#4a3023" />
        <rect x="-9" y="93" width="18" height="16" rx="2" fill="url(#splash-brass)" />
        <path d="M-57 101L-93-121" stroke="#8e6545" stroke-width="8" />
        <path d="M-101-130L-115-117L-131-118L-135-88Q-118-87-107-100L-87-103L-77-118Z"
              fill="url(#splash-steel)" stroke="#9e8066" stroke-width="2" />
        <path d="M-31-51Q-30-84 0-83Q32-83 34-49L26-12L-23-13Z" fill="#583129" />
        <path d="M-25-47Q-21-64 0-65Q20-64 24-47L21-24Q13-9 0-6Q-15-10-22-24Z" fill="url(#splash-skin)" />
        <path d="M-28-48Q-23-83 1-79Q25-76 29-50L16-60L7-58L-4-67L-17-51Z" fill="#8c4933" />
        <path d="M-25-31L-15-15L-1-13L12-17L24-33L21-9L3 6L-16-3Z" fill="#713e2e" />
        <path d="M23-20Q38-12 31 12L41 25L31 35L25 15Z" fill="#713e2e" stroke="#a4734f" stroke-width="2" />
        <path d="M-16-44L-7-43M7-43L16-44M0-42L-3-31L2-30M-6-22Q0-21 6-23" fill="none" stroke="#553023" stroke-width="2.4" stroke-linecap="round" />
        <path d="M-11-39h.1M11-39h.1" stroke="#39271f" stroke-width="2.8" stroke-linecap="round" />
        <path d="M-19-31L-13-39L-9-35" fill="none" stroke="#a65748" stroke-width="2" />
      </g>

      <!-- Wizard: pointed hat, cloak, and silver beard -->
      <g data-class="法师" transform="translate(505 279)">
        <path d="M-39 15L-72 118L-80 194H76L63 112L40 16L11-1H-15Z" fill="url(#splash-cloth)" stroke="#412a29" stroke-width="3" />
        <path d="M-39 17L-11 62L-32 171M40 16L13 62L29 172" fill="none" stroke="#bc8c5a" stroke-width="4" />
        <path d="M-36 76L-51 169M38 75L57 171" fill="none" stroke="#472d2d" stroke-width="4" />
        <path d="M-25-46L-24-25L-13-10H14L26-31L25-52Z" fill="url(#splash-skin)" />
        <path d="M-25-34L-21-14L-29 6L-14 4L-9 24L1 13L13 24L16 6L29 9L22-12L25-34L15-23L0-27L-13-22Z"
              fill="#d7baa0" stroke="#967b6d" stroke-width="2" />
        <path d="M-15-43L-7-42M7-42L15-43M0-39L-3-29L2-28M-5-21L6-21" fill="none" stroke="#5f3a2d" stroke-width="2.3" stroke-linecap="round" />
        <path d="M-11-38h.1M11-38h.1" stroke="#39271f" stroke-width="2.8" stroke-linecap="round" />
        <g transform="translate(0 -8)">
          <path d="M-42-49L-32-68L-18-112Q-11-127 7-132L20-127L-2-110L27-72L43-51Z" fill="#5e3936" stroke="#a77958" stroke-width="2.5" />
          <path d="M-31-70Q-1-61 28-70L36-56Q2-47-37-57Z" fill="#ae8154" />
          <path d="M-62-51Q-10-61 66-53L76-45Q15-34-71-44Z" fill="#553333" stroke="#9e7152" stroke-width="2.5" />
        </g>
        <circle cy="42" r="7" fill="url(#splash-brass)" />
        <path d="M0 38V46M-4 42H4" stroke="#6d4229" stroke-width="1.7" />
      </g>

      <!-- Warrior: steel, long sword, and shield -->
      <g data-class="战士" transform="translate(243 326)">
        <path d="M-45 95L-49 176L-14 176L1 128L13 176H48L49 98Z" fill="#3b302b" />
        <path d="M-50 152L-46 181H-10L-7 159L-25 151ZM15 153L14 181H55L48 157Z" fill="#5e4e43" />
        <path d="M-62 17Q-29-10 0 3Q40-9 62 24L46 99L17 113H-21L-49 99Z"
              fill="url(#splash-steel)" stroke="#3e3330" stroke-width="3.5" />
        <path d="M-24 11L0 24L27 8M-40 38L0 58L41 37M-39 66L0 84L40 65" fill="none" stroke="#ddc4a0" stroke-opacity=".61" stroke-width="2.5" />
        <path d="M-74 28L-51 9L-33 22L-35 45L-71 53L-83 44ZM58 12L83 32L69 53L36 44L38 23Z"
              fill="url(#splash-steel)" stroke="#8c775f" stroke-width="2.5" />
        <path d="M-77 46L-83 87L-62 106L-50 92L-57 48Z" fill="url(#splash-steel)" />
        <path d="M-49 94Q-4 106 48 94L47 109Q0 123-47 109Z" fill="#533426" />
        <rect x="-10" y="98" width="22" height="18" rx="2" fill="url(#splash-brass)" />
        <path d="M-30-40Q-29-70-2-74Q26-75 31-48L29-18H-29Z" fill="#52413a" />
        <path d="M-25-41Q-22-58-1-60Q21-60 25-42L22-18L11-4H-12L-22-17Z" fill="url(#splash-skin)" />
        <path d="M-28-42L-27-62L-15-72L14-71L26-63L29-42L17-54L7-52L-4-59L-12-50L-22-51Z" fill="#574943" />
        <path d="M-21-20L-15-12L-7-11L0-15L7-11L16-12L22-20L17-6L0 0L-17-6Z" fill="#744a38" />
        <path d="M-17-39L-8-40M7-40L17-39M0-39L-3-28L2-27M-6-21H7" fill="none" stroke="#53362b" stroke-width="2.2" stroke-linecap="round" />
        <path d="M-12-35h.1M12-35h.1" stroke="#39271f" stroke-width="2.8" stroke-linecap="round" />
        <path d="M-94 54L-103-112L-90-140L-79-113L-87 54Z" fill="url(#splash-steel)" stroke="#9f886e" stroke-width="2" />
        <path d="M-92-116V48M-113 54H-66M-90 52V82" fill="none" stroke="#d7ae73" stroke-width="4" />
        <path d="M-99 71Q-66 61-38 72L-44 110Q-50 130-67 145Q-89 131-94 111Z"
              fill="#56413a" stroke="#b18960" stroke-width="4" />
        <path d="M-67 83L-55 104L-67 119L-79 104Z" fill="url(#splash-brass)" />
      </g>

      <!-- Rogue: open hood, leather harness, and dagger -->
      <g data-class="盗贼" transform="translate(621 325)">
        <path d="M-29 1Q-72 8-79 127L-61 170L44 168L65 134L59 42L32 5Z" fill="#3e2c2c" />
        <path d="M-42 102L-44 174H-10L2 134L18 176H49L42 99Z" fill="#382b27" />
        <path d="M-41 15L-52 64L-39 111Q1 125 43 106L49 47L34 10Z"
              fill="url(#splash-cloth)" stroke="#49322c" stroke-width="3" />
        <path d="M-42 33L22 116L35 108L-30 25M-43 97L45 84" fill="none" stroke="#b78a5d" stroke-width="5" />
        <path d="M40 22L59 59L64 101L44 103L33 67L29 38Z" fill="#66433a" />
        <path d="M66 99L87 73L87 105L76 126Z" fill="url(#splash-steel)" stroke="#b69c7d" stroke-width="2" />
        <path d="M-39-28Q-41-58-10-77L-1-87Q30-70 40-36L34-8L17 9L-29 2Z"
              fill="#573a3a" stroke="#9b6751" stroke-width="3" />
        <path d="M-25-36Q-11-60 0-63Q18-57 26-36L19-16L5-4L-12-10L-23-23Z"
              fill="url(#splash-skin)" />
        <path d="M-27-37L-15-55L2-60L17-54L25-37L9-50L-4-47L-15-39Z" fill="#51372d" />
        <path d="M-16-34L-7-36M7-36L16-34M0-35L-4-23L2-22M-5-16Q0-14 7-17" fill="none" stroke="#59372a" stroke-width="2.3" stroke-linecap="round" />
        <path d="M-11-31h.1M11-31h.1" stroke="#39271f" stroke-width="2.8" stroke-linecap="round" />
        <circle cx="19" cy="-13" r="3" fill="#d9ae70" />
        <path d="M-35 5Q-1 18 32 1L25 23Q0 36-31 20Z" fill="#76504a" />
        <path d="M-17 71L-4 87M-24 77L-12 93" stroke="#57332b" stroke-width="2.5" />
      </g>

      <!-- Cleric: linen vestments, gilded sun, and staff -->
      <g data-class="牧师" transform="translate(750 318)">
        <path d="M-48 18Q-37 2-9-4H12Q51 6 57 30L53 90L70 173H-58L-46 103L-58 43Z"
              fill="url(#splash-linen)" stroke="#684a35" stroke-width="3.5" />
        <path d="M-32 19L-21 163M34 15L23 165M-17 87L-16 170M18 87L18 170"
              fill="none" stroke="#b88c55" stroke-width="4" />
        <path d="M-57 31L-31 4L-12 9L-23 42L-46 64L-65 54ZM49 22L66 46L54 64L26 39L16 8L32 3Z"
              fill="#c7aa7e" stroke="#997450" stroke-width="2.7" />
        <path d="M55 56L78 83L70 104L42 86L34 68Z" fill="#a68660" />
        <path d="M81 139V-75" stroke="#926a4a" stroke-width="7" />
        <path d="M75 80Q83 74 89 83L88 99L77 101L72 91Z" fill="url(#splash-skin)" />
        <circle cx="81" cy="-87" r="18" fill="#6a4a36" stroke="#d6a961" stroke-width="3.5" />
        <path d="M81-113V-61M55-87H107M62-106L100-68M62-68L100-106" stroke="#c59a60" stroke-width="3" />
        <circle cx="81" cy="-87" r="8" fill="#e2b973" />
        <path d="M-35-31Q-37-69-3-78Q25-79 36-46L35-12L17 0H-21Z" fill="#795841" stroke="#bd9868" stroke-width="2" />
        <path d="M-24-40Q-17-61 1-59Q19-56 25-38L21-19L9-7H-9L-22-22Z" fill="url(#splash-skin)" />
        <path d="M-27-39L-23-58L-7-70L12-67L25-57L27-37L13-49L1-51L-11-51L-19-42Z" fill="#694736" />
        <path d="M-21-55Q-1-66 22-55" fill="none" stroke="#d5b083" stroke-width="3" />
        <path d="M-16-36L-8-37M7-37L16-36M0-36L-3-25L2-24M-6-17Q1-15 7-18" fill="none" stroke="#543529" stroke-width="2.2" stroke-linecap="round" />
        <path d="M-11-32h.1M11-32h.1" stroke="#39271f" stroke-width="2.8" stroke-linecap="round" />
        <path d="M-30-5Q0 12 31-5L35 8Q1 27-34 9Z" fill="#e1c29b" />
        <circle cy="54" r="11" fill="#74513a" stroke="#d5af70" stroke-width="2" />
        <path d="M0 46V62M-8 54H8M-5 49L5 59M5 49L-5 59" stroke="#ecc691" stroke-width="1.6" />
      </g>

      <!-- Chest basin and hinged lid are separate animation targets. -->
      <ellipse class="chest-halo" cx="500" cy="400" rx="380" ry="205" fill="url(#splash-radiance)" />
      <path d="M366 450L387 423H613L634 450L616 475H384Z" fill="#3d281e" stroke="#b18656" stroke-width="5" />
      <path d="M391 435L404 429H597L612 444L592 456H401Z" fill="#201611" />
      <path class="chest-interior" d="M391 435L404 429H597L612 444L592 456H401Z" fill="#f1b762" />
      <g class="chest-lid">
        <path d="M369 448V424Q377 389 414 381H588Q623 388 632 425V448Z"
              fill="url(#splash-oak)" stroke="#2b1b16" stroke-width="4" />
        <path d="M376 421Q390 399 416 394H585Q609 399 624 421M374 435H626"
              fill="none" stroke="#d39a62" stroke-opacity=".55" stroke-width="3" />
        <path d="M389 447V420Q394 393 418 382H433Q412 400 408 421V447ZM590 447V421Q586 397 565 382H584Q608 396 612 420V447Z"
              fill="url(#splash-brass)" stroke="#6a4229" stroke-width="2.5" />
        <path d="M369 440H633V452H369Z" fill="url(#splash-brass)" stroke="#714629" stroke-width="2.5" />
        <path d="M469 443H533" stroke="#e5b779" stroke-width="2" />
      </g>
      <path d="M365 453H635L621 526Q619 535 608 535H390Q379 535 377 526Z" fill="url(#splash-oak)" stroke="#39231a" stroke-width="5" />
      <path d="M380 467H620M383 488H617M387 511H610" stroke="#ba8050" stroke-opacity=".47" stroke-width="2" />
      <path d="M373 460H397L402 529H383ZM607 460H628L617 529H598Z" fill="url(#splash-brass)" stroke="#65432c" stroke-width="2" />
      <path d="M375 450H625L633 461H368ZM380 526H620V537H380Z" fill="url(#splash-brass)" stroke="#6a4329" stroke-width="2.5" />
      <path d="M477 456H523V479Q500 501 477 479Z" fill="url(#splash-brass)" stroke="#68432a" stroke-width="3" />
      <path d="M496 472A4 4 0 1 1 503 474L506 483H494L497 474Z" fill="#4a2e21" />
      <path d="M390 474H391M393 514H394M611 474H612M606 514H607M482 463H483M517 463H518" stroke="#e8c188" stroke-width="3.5" stroke-linecap="round" />

      <!-- The five hands reach toward the chest from their respective silhouettes. -->
      <g class="party-hands" stroke-linejoin="round">
        <path d="M303 358L327 397L375 422L368 437L313 419L289 391Z" fill="url(#splash-steel)" stroke="#826950" stroke-width="3" />
        <path d="M368 421L384 415L398 420L392 429L372 437Z" fill="url(#splash-skin)" stroke="#775039" stroke-width="2" />
        <path d="M347 387L367 412L411 427L417 416L376 387L363 373Z" fill="url(#splash-skin)" stroke="#82513a" stroke-width="2.5" />
        <path d="M402 413L420 407L432 412L428 421L413 427Z" fill="url(#splash-skin)" stroke="#82513a" stroke-width="2" />
        <path d="M471 347L484 384L501 408L487 419L463 395L447 361Z" fill="#775048" stroke="#b78962" stroke-width="2.5" />
        <path d="M487 406L504 402L514 406L509 416L491 421Z" fill="url(#splash-skin)" stroke="#80503a" stroke-width="2" />
        <path d="M582 359L566 393L533 418L547 431L589 410L604 379Z" fill="#6d453e" stroke="#a77556" stroke-width="2.5" />
        <path d="M544 416L529 414L518 420L524 429L542 433Z" fill="url(#splash-skin)" stroke="#775039" stroke-width="2" />
        <path d="M703 366L669 403L625 423L633 438L686 422L717 394Z" fill="url(#splash-linen)" stroke="#a97e58" stroke-width="2.5" />
        <path d="M636 420L617 415L604 420L608 430L628 438Z" fill="url(#splash-skin)" stroke="#775039" stroke-width="2" />
      </g>
    </g>

    <g class="chest-sparks" fill="#ffe3a1">
      <circle cx="390" cy="423" r="2.8" style="--drift: -25px; --rise: -112px" />
      <circle cx="426" cy="425" r="2" style="--drift: -64px; --rise: -75px; --spark-delay: 140ms" />
      <circle cx="457" cy="414" r="3.4" style="--drift: 8px; --rise: -146px; --spark-delay: 210ms" />
      <circle cx="485" cy="431" r="1.9" style="--drift: -40px; --rise: -173px" />
      <circle cx="505" cy="417" r="3" style="--drift: 20px; --rise: -135px; --spark-delay: 90ms" />
      <circle cx="538" cy="418" r="2.2" style="--drift: 54px; --rise: -151px; --spark-delay: 190ms" />
      <circle cx="575" cy="427" r="2.8" style="--drift: 39px; --rise: -92px; --spark-delay: 80ms" />
      <circle cx="605" cy="429" r="2.1" style="--drift: 76px; --rise: -85px; --spark-delay: 190ms" />
      <path d="M444 410l4 5-4 5-4-5Z" style="--drift: -34px; --rise: -127px; --spark-delay: 110ms" />
      <path d="M557 410l4 5-4 5-4-5Z" style="--drift: 47px; --rise: -115px; --spark-delay: 260ms" />
    </g>
  </svg>
</template>

<style scoped>
.splash-art {
  display: block;
  width: min(96vw, 1250px);
  max-height: 91vh;
}

.art-scene { animation: scene-recede 950ms 2360ms both; }

.chest-lid {
  transform-origin: 500px 447px;
  animation: lid-open 1320ms 380ms cubic-bezier(.36, .06, .25, 1) both;
}

.party-hands { animation: hands-reach 1320ms 380ms ease-in-out both; }
.chest-interior { animation: interior-light 1800ms 1260ms ease-out both; }
.chest-halo { animation: halo-spread 1810ms 1260ms ease-in-out both; }

.chest-sparks > * {
  animation: spark-rise 1210ms calc(1480ms + var(--spark-delay, 0ms)) ease-out both;
}

@keyframes lid-open {
  from { transform: translateY(0) scaleY(1); }
  to { transform: translateY(-28px) scaleY(1.12); }
}

@keyframes hands-reach {
  0%, 100% { transform: translateY(0); }
  72% { transform: translateY(-9px); }
}

@keyframes interior-light {
  0% { opacity: 0; }
  35%, 65% { opacity: 1; }
  100% { opacity: .32; }
}

@keyframes halo-spread {
  0% { opacity: 0; }
  36%, 64% { opacity: .76; }
  100% { opacity: 0; }
}

@keyframes spark-rise {
  0% { opacity: 0; }
  17%, 59% { opacity: .9; }
  100% { opacity: 0; transform: translate(var(--drift), var(--rise)); }
}

@keyframes scene-recede {
  from { opacity: 1; }
  to { opacity: .1; }
}
</style>
