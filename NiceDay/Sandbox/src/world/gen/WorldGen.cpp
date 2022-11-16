#include "ndpch.h"
#include "WorldGen.h"
#include "world/World.h"
#include "world/block/block_datas.h"
#include "world/gen/PriorGen.h"


static float clamp(float v, float min, float max)
{
	if (v < min)
		return min;
	if (v > max)
		return max;
	return v;
}

static float smoothstep(float x)
{
	// Scale, bias and saturate x to 0..1 range
	x = clamp(x, 0.0, 1.0);
	// Evaluate polynomial
	return x * x * (3 - 2 * x);
}

static float smootherstep(float x)
{
	// Scale, bias and saturate x to 0..1 range
	x = clamp(x, 0.0, 1.0);
	// Evaluate polynomial
	return x * x * x * (x * (x * 6 - 15) + 10);
}

float WorldGen::getTerrainHeight(int seed, float x)
{
	constexpr float epsilons[] //block distance between two points
	{
		40,
		20,
		10

	};
	constexpr float epsilonMagnitudes[]
	{
		10,
		5,
		2
	};

	float totalHeight = 0;
	for (int i = 0; i < sizeof(epsilons) / sizeof(float); ++i)
	{
		int index0 = (int)(x / epsilons[i]);
		int index1 = index0 + 1;
		//std::srand(seed + (index0 * 489 + 1456 * i) * 1000);
		std::srand(seed + (index0 * 489 + 1456) * 1000);
		float v0 = (std::rand() % 1024) / 1023.f;
		std::srand(seed + (index1 * 489 + 1456) * 1000);
		//std::srand(seed + (index1 * 489 + 1456 * i) * 1000);
		float v1 = (std::rand() % 1024) / 1023.f;

		totalHeight += ((smootherstep((x - index0 * epsilons[i]) / epsilons[i]) * (v1 - v0) + v0) * 2 - 1) *
			epsilonMagnitudes[i];
	}


	return totalHeight;
}

inline static bool& getFromMap(bool* m, int x, int y)
{
	ASSERT(x >= 0 && x < 2*WORLD_CHUNK_SIZE&&y >= 0 && y < 2*WORLD_CHUNK_SIZE, "Invalid chunkgen coords!");
	return m[y * WORLD_CHUNK_SIZE * 2 + x];
}

static int getNeighbourCount(bool* map, int x, int y)
{
	int out = 0;
	for (int xx = x - 1; xx < x + 2; ++xx)
	{
		for (int yy = y - 1; yy < y + 2; ++yy)
		{
			if (xx <= 0 || xx >= 2 * WORLD_CHUNK_SIZE || yy <= 0 || yy >= 2 * WORLD_CHUNK_SIZE)
				continue;
			out += getFromMap(map, xx, yy) ? 0 : 1;
		}
	}
	return out;
}

void WorldGen::genLayer0(World& w,Chunk& c)
{
	//TimerStaper t("gen chunk");
	if (m_prior_gen == nullptr)
	{
		m_prior_gen = new PriorGen("file");
		m_prior_gen->gen(w.getInfo().seed, w.getInfo().terrain_level,
		                 w.getInfo().chunk_width * WORLD_CHUNK_SIZE, w.getInfo().chunk_height * WORLD_CHUNK_SIZE);
	}
	for (int zz = 0; zz < WORLD_CHUNK_SIZE; ++zz)
	{
		for (int ww = 0; ww < WORLD_CHUNK_SIZE; ++ww)
		{
			auto worldx = c.m_x * WORLD_CHUNK_SIZE + ww;
			auto worldy = c.m_y * WORLD_CHUNK_SIZE + zz;
			c.block(ww, zz) = m_prior_gen->getBlock(worldx, worldy);
		}
	}
	int offsetX = c.m_x * WORLD_CHUNK_SIZE;
	int offsetY = c.m_y * WORLD_CHUNK_SIZE;
	half_int index = c.chunkID();

	//update block states
	for (int y = 1; y < WORLD_CHUNK_SIZE - 1; y++)
	{
		for (int x = 1; x < WORLD_CHUNK_SIZE - 1; x++)
			//boundaries will be updated after all other adjacent chunks are generated
		{
			auto& block = c.block(x, y);

			auto worldx = offsetX + x;
			auto worldy = offsetY + y;
			/*if (!block.isAir())
				BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(p, worldx, worldy);
			if (!block.isWallFree())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(p, worldx, worldy);*/
		}
	}
	c.last_save_time = w.getWorldTicks(); //mark it as was generated
}
